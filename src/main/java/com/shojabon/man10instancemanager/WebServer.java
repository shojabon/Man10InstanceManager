package com.shojabon.man10instancemanager;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WebServer extends AbstractHandler {

    public static void boot(int port){
        Server server = new Server(port);
        server.setHandler(new WebServer());
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject createResponse(String status, String message, Object data){
        Map<String, Object> finalResult = new HashMap<>();
        finalResult.put("message", message);
        finalResult.put("status", status);
        finalResult.put("data", data);

        return new JSONObject(finalResult);
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if(!target.equals("/servers")){
            return;
        }

        response.setContentType("application/json;charset=utf-8");
        baseRequest.setHandled(true);

        String method = request.getMethod();

        try{
            if (method.equals("GET")) {
                // handle GET request
                ArrayList<Map<String, Object>> result = new ArrayList<>();
                Map<String, ServerInfo> servers = ProxyServer.getInstance().getServersCopy();
                for(String serverName : servers.keySet()){
                    Map<String, Object> localObject = new HashMap<>();
                    ServerInfo info = servers.get(serverName);
                    localObject.put("name", serverName);
                    localObject.put("host", info.getSocketAddress().toString().split(":")[0]);
                    localObject.put("port", Integer.parseInt(info.getSocketAddress().toString().split(":")[1]));
                    localObject.put("motd", info.getMotd());

                    result.add(localObject);
                }

                response.setStatus(200);
                response.getWriter().println(createResponse("success", "Success", result));
                return;
            } else if (method.equals("POST") || method.equals("DELETE")) {
                // handle POST request
                BufferedReader reader = request.getReader();
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                JSONObject json = new JSONObject(sb.toString());

                if(method.equals("POST")){
                    if(!json.has("name") && !json.has("host") && !json.has("port")){
                        response.setStatus(422);
                        response.getWriter().println(createResponse("post_body_invalid", "Invalid JSON post body.", new ArrayList<>()));
                        return;
                    }
                    ServerInfo info = ProxyServer.getInstance().constructServerInfo(((String) json.get("name")), new InetSocketAddress((String) json.get("host"), (Integer) json.get("port")), "", false);
                    ProxyServer.getInstance().getServers().put((String) json.get("name"), info);

                    response.setStatus(200);
                    response.getWriter().println(createResponse("success", "Success", new ArrayList<>()));
                    return;
                }

                if(method.equals("DELETE")){
                    if(!json.has("name")){

                        response.setStatus(422);
                        response.getWriter().println(createResponse("post_body_invalid", "Invalid JSON post body.", new ArrayList<>()));
                        return;
                    }
                    ProxyServer.getInstance().getServers().remove((String) json.get("name"));

                    response.setStatus(200);
                    response.getWriter().println(createResponse("success", "Success", new ArrayList<>()));
                    return;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().println(createResponse("error_internal", "Internal error occurred.", new ArrayList<>()));
        }
    }
}