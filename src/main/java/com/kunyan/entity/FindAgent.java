package com.kunyan.entity;

import java.util.List;

public class FindAgent {
    private List<String> findDownList;
    private List<String> findRegList;
    private List<FindRegDirection> direction;
    public enum FindRegDirection {
        HEAD,
        TAIL
    }
}
