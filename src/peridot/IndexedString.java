/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot;

/**
 *
 * @author pentalpha
 */
public class IndexedString implements Comparable<IndexedString>{

    private String text;
    private int number;

    public IndexedString(int number, String text) {
        this.text = text;
        this.number = number;
    }

    public String getText() {
        return text;
    }
    
    public int getNumber(){
        return number;
    }

    @Override
    public String toString() {
        return Integer.toString(number) + " - " + getText();
    }
    
    @Override
    public int compareTo(IndexedString x) {
        if(this.number < x.getNumber()){
            return -1;
        }else if(this.number > x.getNumber()){
            return 1;
        }else{
            return 0;
        }
    }
}