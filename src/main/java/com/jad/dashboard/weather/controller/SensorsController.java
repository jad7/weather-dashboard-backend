package com.jad.dashboard.weather.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jad.dashboard.weather.controller.dto.SensorValue;
import com.jad.dashboard.weather.dao.model.SensorPoint;
import com.jad.dashboard.weather.provider.SensorEvent;
import com.jad.dashboard.weather.provider.SensorService;
import com.jad.dashboard.weather.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

@Slf4j
@Component
@RestController
@RequestMapping(SensorsController.SENSORS_V1)
public class SensorsController  extends TextWebSocketHandler implements ApplicationListener<SensorEvent> {
    public static final String SENSORS_V1 = "/sensors/v1/";

    private final ObjectMapper mapper;
    private final SensorService sensorService;

    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public SensorsController(ObjectMapper mapper, SensorService sensorService) {
        this.mapper = mapper;
        this.sensorService = sensorService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        super.handleTransportError(session, exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
    }

    @Override
    public void onApplicationEvent(SensorEvent event) {

    }

    @GetMapping("day")
    public Map<String, List<SensorValue>> getForDay(
            @RequestParam(required = false) String sensors) {
        List<String> sensorsList = null;
        if (sensors != null && !sensors.trim().isEmpty()) {
            sensorsList = Arrays.asList(StringUtils.tokenizeToStringArray(sensors, ",", true, true));
        }
        Stream<SensorPoint> stream = sensorService.getFromBuffer(sensorsList, Instant.now().minus(1, ChronoUnit.DAYS));
        return stream.reduce(new HashMap<>(), (map, sp) -> {
                map.computeIfAbsent(sp.getSensorName(), k -> new ArrayList<>()).add(new SensorValue(Utils.roundF(sp.getValue()), sp.getTime()));
                return map;
        }, (map, map2) -> map);

    }




}
