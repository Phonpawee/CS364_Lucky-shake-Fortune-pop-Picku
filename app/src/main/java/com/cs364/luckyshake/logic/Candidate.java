package com.cs364.luckyshake.logic;

/**
 * One item DrawEngine can pick. The engine only needs an id and a weight, so it does not
 * depend on the database model. The ViewModel turns each Menu into Candidate(menu.id, menu.weight).
 */
public final class Candidate {
    public final long id;
    public final int weight;

    public Candidate(long id, int weight) {
        this.id = id;
        this.weight = weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Candidate)) return false;
        Candidate c = (Candidate) o;
        return id == c.id && weight == c.weight;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id) * 31 + weight;
    }

    @Override
    public String toString() {
        return "Candidate{id=" + id + ", weight=" + weight + "}";
    }
}