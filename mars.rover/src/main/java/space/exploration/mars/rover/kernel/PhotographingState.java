package space.exploration.mars.rover.kernel;

import com.google.protobuf.InvalidProtocolBufferException;
import com.yammer.metrics.core.Meter;
import communications.protocol.ModuleDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.exploration.communications.protocol.InstructionPayloadOuterClass;
import space.exploration.communications.protocol.communication.RoverStatusOuterClass;
import space.exploration.communications.protocol.service.CameraPayload;
import space.exploration.communications.protocol.service.WeatherQueryOuterClass;
import space.exploration.communications.protocol.softwareUpdate.SwUpdatePackageOuterClass;
import space.exploration.kernel.diagnostics.LogRequest;
import space.exploration.mars.rover.animation.CameraAnimationEngine;
import space.exploration.mars.rover.environment.MarsArchitect;
import space.exploration.mars.rover.service.PhotoQueryService;
import space.exploration.mars.rover.utils.CameraUtil;
import space.exploration.mars.rover.utils.RoverUtil;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by sanketkorgaonkar on 5/9/17.
 */
public class PhotographingState implements State {
    private Meter  requests = null;
    private Logger logger   = LoggerFactory.getLogger(PhotographingState.class);
    private Rover  rover    = null;

    public PhotographingState(Rover rover) {
        this.rover = rover;
        requests = this.rover.getMetrics().newMeter(PhotographingState.class, getStateName(), "requests", TimeUnit
                .HOURS);
    }

    @Override
    public void requestLogs(LogRequest.LogRequestPacket logRequestPacket) {
    }

    public void receiveMessage(byte[] message) {
        rover.reflectRoverState();
        logger.debug("Photographing state received message. Saving to instruction queue");
        rover.getInstructionQueue().add(message);
        try {
            RoverUtil.writeSystemLog(rover, InstructionPayloadOuterClass.InstructionPayload.parseFrom(message), rover
                    .getInstructionQueue().size());
        } catch (InvalidProtocolBufferException ipe) {
            RoverUtil.writeErrorLog(rover, "Invalid Protocol Buffer Exception", ipe);
        }
    }

    @Override
    public void updateSoftware(SwUpdatePackageOuterClass.SwUpdatePackage swUpdatePackage) {
        logger.error("Can not update software in " + getStateName());
    }

    @Override
    public void gracefulShutdown() {
        logger.error(" Can not perform gracefulShutdown while in " + getStateName());
    }

    @Override
    public void synchronizeClocks(String utcTime) {
        logger.debug("Can not sync clocks in " + getStateName());
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
    public void shootNeutrons() {
    }

    @Override
    public Meter getRequests() {
        return requests;
    }

    @Override
    public void activateCameraById(String camId) {
        requests.mark();
        rover.reflectRoverState();
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
        photoQueryService.setSol(rover.getSpacecraftClock().getSol());
        photoQueryService.setAuthenticationKey(rover.getRoverConfig().getNasaApiAuthKey());
        photoQueryService.executeQuery();

        String responseString = photoQueryService.getResponseAsString();
        logger.debug("Query String::" + photoQueryService.getQueryString());
        logger.debug("Response String::" + responseString);
        RoverUtil.writeSystemLog(rover, responseString, rover.getInstructionQueue().size());

        CameraPayload.CamPayload camPayload = null;
        try {
            camPayload =
                    CameraUtil.convertToCamPayload(responseString, rover.getRoverConfig().getDataArchiveLocation()
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
                .setSolNumber(rover.getSpacecraftClock().getSol()).setLocation(location).setNotes("Camera used here")
                .setSCET(System
                                 .currentTimeMillis())
                .setNotes("Curiosity Actual")
                .setModuleMessage(camPayload.toByteString())
                .setModuleReporting(ModuleDirectory.Module.CAMERA_SENSOR.getValue());

        if (camPayload != null) {
            rBuilder.setModuleMessage(camPayload.toByteString());
            int lifeSpan = rover.getCamera().getLifeSpan();
            rover.getCamera().setLifeSpan(--lifeSpan);
        }

        rover.setState(rover.getTransmittingState());
        rover.transmitMessage(rBuilder.build().toByteArray());
    }

    public void move(InstructionPayloadOuterClass.InstructionPayload.TargetPackage targetPackage) {
        logger.debug("Can not move in " + getStateName());
    }

    public void hibernate() {
    }

    public void senseWeather(WeatherQueryOuterClass.WeatherQuery weatherQuery) {
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

    @Override
    public void getSclkInformation() {
        logger.error("Can not get sclkInformation while in photgraphingState");
    }
}
