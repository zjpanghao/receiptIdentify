package com.kunyan.service;

import com.kunyan.entity.LocationOcr;

import java.util.List;

public interface TeService {
    LocationOcr findSplitLocation(List<LocationOcr> locationOcrs, List<String> keys);
    List<LocationOcr> findIdentifyWords(List<LocationOcr> locationOcrs, List<String> keys);
    List<LocationOcr> findCompareWords(List<LocationOcr> locationOcrs, List<String> values);
    List<LocationOcr> [] splitLocationOcrs(List<LocationOcr> locationOcrs, int x);
}
