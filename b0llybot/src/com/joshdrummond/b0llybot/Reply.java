package com.joshdrummond.b0llybot;

public class Reply {

    private int chance;
    private String[] answers;
    
    public Reply(int chance, String... answers)
    {
        assert (chance >= 0 && chance <= 100);
        this.chance = chance;
        this.answers = answers;
    }
    
    public int getChance()
    {
        return chance;
    }
    
    public String[] getAnswers()
    {
        return answers;
    }
}
