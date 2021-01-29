package io.github.idoomful.assassinseconomy.data.SQL;

import java.util.LinkedHashMap;

public class MapWrapper {
    private LinkedHashMap<String, Integer> map;

    public MapWrapper(LinkedHashMap<String, Integer> map) {
        this.map = map;
    }

    public LinkedHashMap<String, Integer> getMap() {
        return map;
    }

    public void setMap(LinkedHashMap<String, Integer> map) {
        this.map = map;
    }
}
