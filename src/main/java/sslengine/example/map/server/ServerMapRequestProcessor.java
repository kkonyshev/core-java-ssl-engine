package sslengine.example.map.server;

import org.apache.log4j.Logger;
import sslengine.example.map.dto.MtTransferReq;
import sslengine.example.map.dto.MtTransferRes;
import sslengine.example.map.dto.ResultStatus;
import sslengine.example.simpleobject.dto.SimpleRequestDto;
import sslengine.example.simpleobject.dto.SimpleResponseDto;
import sslengine.server.ServerRequestProcessor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerMapRequestProcessor implements ServerRequestProcessor<MtTransferReq, MtTransferRes> {

    protected final Logger LOG = Logger.getLogger(getClass());

    private ConcurrentHashMap<String, List<Map<Object, Object>>> holder = new ConcurrentHashMap<>();

    @Override
    public MtTransferRes process(MtTransferReq mtTransferRes) {
        Map<Object, Object> map = new HashMap<>();

        switch (mtTransferRes.event) {
            case START:
                LOG.info("starting process: " + mtTransferRes.processId);
                holder.put(mtTransferRes.processId, new ArrayList<>());
                break;
            case PROCESS:
                LOG.info("putting object: " + mtTransferRes.entry);
                holder.get(mtTransferRes.processId).add(mtTransferRes.entry);
                break;
            case END:
                LOG.info("finishing process: " + mtTransferRes.processId);
                List<Map<Object, Object>> result = holder.remove(mtTransferRes.processId);
                LOG.info("result map: " + result);
                break;
        }

        return new MtTransferRes(mtTransferRes.processId, ResultStatus.OK);
    }
}
