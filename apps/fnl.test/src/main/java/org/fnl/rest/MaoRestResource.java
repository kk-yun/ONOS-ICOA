package org.fnl.rest;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.fnl.intf.MaoRoutingService;
import org.onosproject.incubator.net.PortStatisticsService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkService;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.Set;

/**
 * Mao Test Label.
 */
@Path("mao")
public class MaoRestResource extends AbstractWebResource {

    private MaoRoutingService maoRoutingService = get(MaoRoutingService.class);
    private LinkService linkService = get(LinkService.class);
    private PortStatisticsService portStatisticsService = get(PortStatisticsService.class);
    private DeviceService deviceService = get(DeviceService.class);

    /**
     * Hello world.
     * Mao.
     * @return Beijing
     */
    @GET
    @Path("hello")
    public Response hello(){
        ObjectNode root = mapper().createObjectNode();
        root.put("Hello", 1080)
                .put("Mao",7181);

        ArrayNode array = root.putArray("RadioStation");
        array.add("192.168.1.1").add("127.0.0.1").add("10.3.8.211");

        return ok(root).build();
    }


    @GET
    @Path("getLinksLoad")
    public Response getLinksLoad(){

        Set<String> linksList = new HashSet<>();

        ObjectNode root = mapper().createObjectNode();
        ArrayNode capabilities = root.putArray("LinkCapability");

        linkService.getLinks().forEach(link -> {


            ConnectPoint linkSrcPort = link.src();
            ConnectPoint linkDstPort = link.dst();

            if(isEnrolled(linksList, linkSrcPort.deviceId(), linkDstPort.deviceId()))
                return;

            long srcPortLineSpeed = getPortLineSpeed(linkSrcPort);
            long dstPortLineSpeed = getPortLineSpeed(linkDstPort);
            assert srcPortLineSpeed == dstPortLineSpeed;

            long srcPortLoadSpeed = getPortLoadSpeed(linkSrcPort);
            long dstPortLoadSpeed = getPortLoadSpeed(linkDstPort);

            long srcPortRestSpeed = srcPortLineSpeed - srcPortLoadSpeed;
            long dstPortRestSpeed = dstPortLineSpeed - dstPortLoadSpeed;



            long linkLineSpeed = srcPortLineSpeed;
            long linkLoadSpeed = getLinkLoadSpeed(srcPortLoadSpeed, dstPortLoadSpeed);
            long linkRestSpeed = getLinkRestSpeed(srcPortRestSpeed, dstPortRestSpeed);

            ObjectNode linkNode = mapper().createObjectNode();
            linkNode.put("Name", (linkSrcPort.deviceId().toString()+ "<->"+linkDstPort.deviceId().toString()).replace("0",""));
            linkNode.put("Line", linkLineSpeed);
            linkNode.put("Load", linkLoadSpeed);
            linkNode.put("Rest", linkRestSpeed);



            capabilities.add(linkNode);
        });

        return ok(root).build();
    }

    /**
     * Unit: bps
     * @param port
     * @return
     */
    private long getPortLoadSpeed(ConnectPoint port){

        return portStatisticsService.load(port).rate() * 8;//data source: Bps

    }

    /**
     * Unit bps
     * @param port
     * @return
     */
    private long getPortLineSpeed(ConnectPoint port){

        assert port.elementId() instanceof DeviceId;
        return deviceService.getPort(port.deviceId(),port.port()).portSpeed() * 1000000;//data source: Mbps

    }

    private long getLinkLoadSpeed(long src, long dst){
        return src > dst ? src : dst;
    }

    private long getLinkRestSpeed(long src, long dst){
        return src < dst ? src : dst;
    }
    private boolean isEnrolled(Set<String> linkList, DeviceId a, DeviceId b){

        if(linkList.contains(a.toString()+b.toString()) ||
                linkList.contains(b.toString()+a.toString())){
            return true;
        } else {
            linkList.add(a.toString()+b.toString());
            return false;
        }
    }
}
