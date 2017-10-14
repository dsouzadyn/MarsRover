package space.exploration.mars.rover.kernel;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.exploration.mars.rover.InstructionPayloadOuterClass;
import space.exploration.mars.rover.animation.CameraAnimationEngine;
import space.exploration.mars.rover.communication.RoverStatusOuterClass;
import space.exploration.mars.rover.dataUplink.CameraPayload;
import space.exploration.mars.rover.dataUplink.PhotoQueryService;
import space.exploration.mars.rover.environment.MarsArchitect;
import space.exploration.mars.rover.utils.CameraUtil;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Created by sanketkorgaonkar on 5/9/17.
 */
public class PhotographingState implements State {
    private Logger logger = LoggerFactory.getLogger(PhotographingState.class);
    private Rover  rover  = null;

    public PhotographingState(Rover rover) {
        this.rover = rover;
    }

    public void receiveMessage(byte[] message) {
        logger.debug("Photographing state received message. Saving to instruction queue");
        rover.getInstructionQueue().add(message);
        try {
            rover.writeSystemLog(InstructionPayloadOuterClass.InstructionPayload.parseFrom(message), rover
                    .getInstructionQueue().size());
        } catch (InvalidProtocolBufferException ipe) {
            rover.writeErrorLog("Invalid protocolBuffer Exception", ipe);
        }
    }

    @Override
    public String getStateName() {
        return "Photographing State";
    }

    public void transmitMessage(byte[] message) {

    }

    public void exploreArea() {

    }

    @Override
    public void activateCameraById(String camId) {
        MarsArchitect marsArchitect = rover.getMarsArchitect();

        CameraAnimationEngine cameraAnimationEngine = marsArchitect.getCameraAnimationEngine(marsArchitect
                                                                                                     .getRobot()
                                                                                                     .getLocation
                                                                                                             ());
        cameraAnimationEngine.setMarsSurface(marsArchitect.getMarsSurface());
        cameraAnimationEngine.setRobot(marsArchitect.getRobot());
        cameraAnimationEngine.clickCamera();

        PhotoQueryService photoQueryService = new PhotoQueryService();
        photoQueryService.setCamId(camId);
        photoQueryService.setAuthenticationKey(rover.getNasaApiAuthKey());

        int offsetDays = ThreadLocalRandom.current().nextInt(7, 31);
        photoQueryService.setEarthStartDate(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(offsetDays));
        photoQueryService.executeQuery();

        String responseString = photoQueryService.getResponseAsString();
        logger.debug("Query String::" + photoQueryService.getQueryString());
        logger.debug("Response String::" + responseString);
        rover.writeSystemLog(responseString, rover.getInstructionQueue().size());

        CameraPayload.CamPayload camPayload = null;
        try {
            camPayload =
                    CameraUtil.convertToCamPayload(responseString, rover.getDataArchiveLocation()
                            + "/" + camId);
        } catch (IOException e) {
            logger.error("IOException here", e);
        }

        RoverStatusOuterClass.RoverStatus.Location location = RoverStatusOuterClass.RoverStatus.Location
                .newBuilder()
                .setX(marsArchitect.getRobot().getLocation().x)
                .setY(marsArchitect.getRobot().getLocation().y).build();

        RoverStatusOuterClass.RoverStatus.Builder rBuilder = RoverStatusOuterClass.RoverStatus.newBuilder();

        rBuilder.setBatteryLevel(rover.getBattery()
                                         .getPrimaryPowerUnits())
                .setSolNumber(rover.getSol()).setLocation(location).setNotes("Camera used here")
                .setSCET(System
                                 .currentTimeMillis())
                .setNotes("Curiosity Actual")
                .setModuleMessage(camPayload.toByteString())
                .setModuleReporting(ModuleDirectory.Module.CAMERA_SENSOR.getValue());

        if (camPayload != null) {
            rBuilder.setModuleMessage(camPayload.toByteString());
        }

        rover.setState(rover.getTransmittingState());
        rover.transmitMessage(rBuilder.build().toByteArray());
    }

    public void activateCamera() {
        MarsArchitect marsArchitect = rover.getMarsArchitect();
        byte[]        cameraShot    = rover.getCamera().takePhoto(marsArchitect.getRobot().getLocation());

            /* Do not render animation in case of sensor endOfLife */
        if (!rover.isEquipmentEOL()) {
            CameraAnimationEngine cameraAnimationEngine = marsArchitect.getCameraAnimationEngine(marsArchitect
                                                                                                         .getRobot()
                                                                                                         .getLocation

                                                                                                                 ());
            cameraAnimationEngine.setMarsSurface(marsArchitect.getMarsSurface());
            cameraAnimationEngine.setRobot(marsArchitect.getRobot());
            cameraAnimationEngine.clickCamera();

            RoverStatusOuterClass.RoverStatus.Location location = RoverStatusOuterClass.RoverStatus.Location
                    .newBuilder()
                    .setX(marsArchitect.getRobot().getLocation().x)
                    .setY(marsArchitect.getRobot().getLocation().y).build();

            RoverStatusOuterClass.RoverStatus.Builder rBuilder = RoverStatusOuterClass.RoverStatus.newBuilder();

            RoverStatusOuterClass.RoverStatus status = null;
            if (cameraShot != null) {
                System.out.println("Camera shot was not null");
                status = rBuilder.setBatteryLevel(rover.getBattery()
                                                          .getPrimaryPowerUnits())
                        .setSolNumber(rover.getSol()).setLocation(location).setNotes("Camera used here")
                        .setModuleMessage(ByteString.copyFrom(cameraShot)).setSCET(System
                                                                                           .currentTimeMillis())
                        .setModuleReporting(ModuleDirectory.Module.CAMERA_SENSOR.getValue()).build();
            } else {
                System.out.println("Camera shot was null");
                status = rBuilder.setBatteryLevel(rover.getBattery()
                                                          .getPrimaryPowerUnits())
                        .setSolNumber(rover.getSol()).setLocation(location).setNotes("Camera wasn't able to take " +
                                                                                             "a " +
                                                                                             "shot. " +
                                                                                             "Sorry earth!")
                        .setSCET(System
                                         .currentTimeMillis())
                        .setModuleReporting(ModuleDirectory.Module.CAMERA_SENSOR.getValue()).build();
            }

            logger.debug(status.toString());

            rover.getMarsArchitect().returnSurfaceToNormal();
            rover.state = rover.transmittingState;
            rover.transmitMessage(status.toByteArray());
        }

            /* Flip the flag so the sensor can perform its last operation */
        rover.setEquipmentEOL(false);
    }

    public void move(InstructionPayloadOuterClass.InstructionPayload payload) {
    }


    public void hibernate() {

    }

    public void senseWeather() {

    }

    public void scanSurroundings() {

    }

    public void activateCameraById() {

    }

    public void performRadarScan() {

    }

    @Override
    public void sleep() {

    }

    @Override
    public void wakeUp() {

    }
}
