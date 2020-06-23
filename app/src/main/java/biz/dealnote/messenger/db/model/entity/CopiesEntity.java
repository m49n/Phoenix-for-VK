package biz.dealnote.messenger.db.model.entity;

import java.util.List;

import biz.dealnote.messenger.db.model.IdPairEntity;


public class CopiesEntity {

    private int count;

    private List<IdPairEntity> pairDbos;

    public int getCount() {
        return count;
    }

    public CopiesEntity setCount(int count) {
        this.count = count;
        return this;
    }

    public List<IdPairEntity> getPairDbos() {
        return pairDbos;
    }

    public CopiesEntity setPairDbos(List<IdPairEntity> pairDbos) {
        this.pairDbos = pairDbos;
        return this;
    }
}