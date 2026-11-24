package com.kirill.simplesorter.sorting;
/** Separate projection for Sort by Mod. Never alters the Smart profile. */
public final class ModOriginProfile {
    private ModOriginProfile(){}
    public static String namespace(String id){
        int colon=id.indexOf(':');
        return colon<0?"minecraft":id.substring(0,colon);
    }
    public static SemanticItemProfile of(SemanticItemProfile source){
        String key="mod/"+namespace(source.itemId());
        String order=source.group()+"/"+source.legacyFamily()+"/"+source.material()+"/"+source.form();
        return new SemanticItemProfile(source.itemId(),source.category(),key,key,"",order,key,
                source.moveAllowed(),source.confidence(),source.evidence());
    }
}
