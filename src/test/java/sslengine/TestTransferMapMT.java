package sslengine;

import sslengine.client.ClientConnectionFactory;
import sslengine.client.SSLClientConnectionFactory;
import sslengine.example.map.client.MtMapClientHandler;
import sslengine.example.map.client.MtMapClientImpl;
import sslengine.example.map.dto.MtTransferReq;
import sslengine.example.map.dto.MtTransferRes;
import sslengine.example.map.dto.TransferEvent;
import sslengine.example.map.server.MtMapServerSocketProcessorFactory;
import sslengine.server.SSLServerConnectionAcceptor;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TestTransferMapMT extends BaseSSLTest {

    @org.junit.Test
    public void testMultiThread() throws Exception {
        ServerProcess server = ServerProcess.createInstance(new SSLServerConnectionAcceptor("localhost", 9223, new MtMapServerSocketProcessorFactory(), serverContext));

        ClientConnectionFactory clientConnectionFactory = SSLClientConnectionFactory.buildFactory("localhost", 9223, clientContext);
        Executor executor = Executors.newFixedThreadPool(3);

        String processId = UUID.randomUUID().toString();

        MtMapClientImpl initClient = new MtMapClientImpl(clientConnectionFactory, new MtMapClientHandler());
        initClient.start(processId);

        Map<Object, Object> sourceMap = new HashMap<>();
        sourceMap.put(1, 2);
        sourceMap.put(2, 3);
        sourceMap.put(4, 1);
        sourceMap.put("key", "value");
        sourceMap.put("long-key", "value2");
        sourceMap.put(3, 0);
        sourceMap.put(40, 22);
        sourceMap.put(31, 2);
        sourceMap.put(34, 1);

        int threadCount = 3;
        Map<Integer, List<Map<Object, Object>>> threadMap = prepareMapForTransfering(threadCount, sourceMap);

        for (Map.Entry<Integer, List<Map<Object, Object>>> thE: threadMap.entrySet()) {
            executor.execute(() -> {
                LOG.debug("started thread="+thE.getKey());
                MtMapClientImpl threadClient = null;
                try {
                    threadClient = new MtMapClientImpl(clientConnectionFactory, new MtMapClientHandler());
                    for(Map<Object, Object> mm: thE.getValue()) {
                        MtTransferReq requestDto = new MtTransferReq(processId, TransferEvent.PROCESS, mm);
                        LOG.debug("REQ: " + requestDto);
                        MtTransferRes responseDto = threadClient.call(requestDto);
                        LOG.debug("RES: " + responseDto);
                    }
                } catch (Exception e1) {
                    LOG.error(e1.getMessage(), e1);
                } finally {
                    if (threadClient!=null) {
                        try {
                            threadClient.shutdown();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    LOG.debug("stopped thread="+thE.getKey());
                }
            });
        }


        Thread.sleep(3000);

        initClient.stop(processId);

        server.stop();
    }

    private Map<Integer, List<Map<Object, Object>>> prepareMapForTransfering(int threadCount, Map<Object, Object> sourceMap) {
        List<List<Map<Object, Object>>> listMap = new ArrayList<>();

        int threshold = sourceMap.size()/threadCount + 1;
        Iterator<Map.Entry<Object, Object>> mI = sourceMap.entrySet().iterator();
        while (mI.hasNext()) {
            List<Map<Object, Object>> innerMapList = new ArrayList<>();
            for (int j=0; j<threshold && mI.hasNext(); j++) {
                Map<Object, Object> map = new HashMap<>();
                Map.Entry<Object, Object> entry = mI.next();
                map.put(entry.getKey(), entry.getValue());
                innerMapList.add(map);
            }
            if (!innerMapList.isEmpty()) {
                listMap.add(innerMapList);
            }
        }

        Map<Integer, List<Map<Object, Object>>> threadMap = new HashMap<>();

        for (int i=0; i< listMap.size(); i++) {
            threadMap.put(i, listMap.get(i));
        }
        return threadMap;
    }

}
