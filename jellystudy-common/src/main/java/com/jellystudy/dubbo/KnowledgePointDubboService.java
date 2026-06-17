package com.jellystudy.dubbo;

import com.jellystudy.entity.KnowledgePoint;

import java.util.List;

public interface KnowledgePointDubboService {
    KnowledgePoint create(KnowledgePoint knowledgePoint);
    
    KnowledgePoint findById(String id);
    
    List<KnowledgePoint> findAll();
    
    KnowledgePoint update(KnowledgePoint knowledgePoint);
    
    void delete(String id);
    
    List<KnowledgePoint> findByParentId(String parentId);
    
    KnowledgePoint findByName(String name);
    
    long count();
}