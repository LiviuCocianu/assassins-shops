package io.github.idoomful.assassinseconomy.data.SQL;

import java.util.HashMap;

public class MapWrapper {
    private HashMap<String, Integer> map;

    public MapWrapper(HashMap<String, Integer> map) {
        this.map = map;
    }

    public HashMap<String, Integer> getMap() {
        return map;
    }

    public void setMap(HashMap<String, Integer> map) {
        this.map = map;
    }
}
