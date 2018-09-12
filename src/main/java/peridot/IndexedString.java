/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot;

/**
 * A Comparable String, with an integer index.
 * @author pentalpha
 */
public class IndexedString implements Comparable<IndexedString>{

    private String text;
    private int number;
    private static int largestNumber = 0;

    public IndexedString(int number, String text) {
        this.text = text;
        this.number = number;
        checkIfGreater();
    }

    public void setIndex(int newValue){
        this.number = newValue;
        checkIfGreater();
    }

    public String getText() {
        return text;
    }
    
    public int getNumber(){
        return number;
    }

    @Override
    public String toString() {
        String numberStr = Integer.toString(number);
        int lenDif = Integer.toString(largestNumber).length() - numberStr.length();
        while(lenDif > 0){
            numberStr = "0" + numberStr;
            lenDif -= 1;
        }
        return numberStr + ". " + getText();
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

    private void checkIfGreater(){
        int absolute = number;
        if(number < 0){
            absolute = -number;
        }
        if (absolute > IndexedString.largestNumber){
            largestNumber = absolute;
        }
    }
}