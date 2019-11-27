package com.virex.e1forum.common;

import java.util.Map;

public class BBCodeConverter {
    public static String toBBCode(String input){
        String bbcode = input;

        for (Map.Entry entry: BBCodeMaps.getHTMLMap().entrySet())
        {
            bbcode = bbcode.replaceAll(entry.getKey().toString(), entry.getValue().toString());
        }

        return bbcode;
    }
}
