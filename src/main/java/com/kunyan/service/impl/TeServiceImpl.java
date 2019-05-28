package com.kunyan.service.impl;

import com.kunyan.entity.IdentifyException;
import com.kunyan.entity.LocationOcr;
import com.kunyan.service.TeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TeServiceImpl implements TeService {


    private String getFirstNumberString(String s) {
        int i = 0;
        int start = -1;
        int end = s.length();
        for (i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                start = i;
                break;
            }
        }

        if (start == -1) {
            return null;
        }
        return  s.substring(start);
    }

    @Override
    public LocationOcr findSplitLocation(List<LocationOcr> locationOcrs, List<String> keys) {
        LocationOcr findOcr = null;
        for (String key : keys) {
            for (LocationOcr ocr : locationOcrs) {
                if (ocr.getValue().contains(key)) {
                    // try find the rightest one
                    if (findOcr == null || findOcr.getX() < ocr.getX()) {
                        findOcr = ocr;
                    }
                }
            }
        }
        return findOcr;
    }

    @Override
    public List<LocationOcr> findIdentifyWords(List<LocationOcr> locationOcrs, List<String> keys) {
        List<LocationOcr> locationOcrList = new ArrayList<>();
        Set<String> flagSet = new HashSet();
        for (LocationOcr locationOcr: locationOcrs) {
            for (String key : keys) {
                Pattern identify = Pattern.compile(key, Pattern.CASE_INSENSITIVE);
                Matcher matcher = identify.matcher(locationOcr.getValue());
                int inx = -1;
                if (matcher.find()) {
                    inx = matcher.regionStart();
                }
                if (inx != -1) {
                    if (!flagSet.contains(key)) {
                        String value = locationOcr.getValue().substring(inx);
                        String findValue = getFirstNumberString(value);
                        if (findValue == null) {
                            continue;
                        }
                        LocationOcr find = new LocationOcr(locationOcr);
                        find.setFindKey(key);
                        find.setFindValue(findValue);
                        locationOcrList.add(find);
                        flagSet.add(key);
                        if (locationOcrList.size() >= keys.size()) {
                            break;
                        }
                    }
                }
            }
        }
        return locationOcrList;
    }

    @Override
    public List<LocationOcr> findCompareWords(List<LocationOcr> locationOcrs, List<String> values) {
        List<LocationOcr> locationOcrList = new ArrayList<>();
        Set<String> flagSet = new HashSet();
        for (LocationOcr locationOcr: locationOcrs) {
            for (String key : values) {
                if (locationOcr.getValue().equals(key)) {
                    if (!flagSet.contains(key)) {
                        locationOcr.setFindValue(key);
                        locationOcrList.add(locationOcr);
                        flagSet.add(key);
                        if (locationOcrList.size() >= values.size()) {
                            break;
                        }
                    }
                }
            }
        }
        return locationOcrList;
    }

    @Override
    public List<LocationOcr>[] splitLocationOcrs(List<LocationOcr> locationOcrs, int x) {
        List<LocationOcr> [] ocrs = new ArrayList[2];
        ocrs[0] = new ArrayList<>();
        ocrs[1] = new ArrayList<>();
        for (LocationOcr ocr : locationOcrs) {
            ocrs[ocr.getX() < x ? 0 : 1].add(ocr);
        }
        return ocrs;
    }
}
