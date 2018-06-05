package peridot.script.r;

import java.io.Serializable;

public class VersionNumber implements Comparable<VersionNumber>, Serializable{
    public int[] numbers;
    private String fullVersion;

    public VersionNumber(String version){
        version = version.replace('.','-');
        String[] strNumbers = version.split("-");
        numbers = new int[strNumbers.length];
        fullVersion = "";
        for(int i = 0; i < strNumbers.length; i++){
            numbers[i] = Integer.parseInt(strNumbers[i]);
            fullVersion = fullVersion + strNumbers[i];
            if(i != strNumbers.length-1){
                fullVersion = fullVersion + ".";
            }
        }
    }

    @Override
    public String toString(){
        return fullVersion;
    }

    @Override
    public int compareTo(VersionNumber versionNumber) {
        int[] localNumbers;
        int[] otherNumbers;
        if (versionNumber.numbers.length > numbers.length){
            localNumbers = new int[versionNumber.numbers.length];
            otherNumbers = versionNumber.numbers;
            for(int i = 0; i < localNumbers.length; i++){
                if(i < numbers.length){
                    localNumbers[i] = numbers[i];
                }else{
                    localNumbers[i] = 0;
                }
            }
        }else if(versionNumber.numbers.length < numbers.length){
            otherNumbers = new int[numbers.length];
            localNumbers = numbers;
            for(int i = 0; i < otherNumbers.length; i++){
                if(i < versionNumber.numbers.length){
                    otherNumbers[i] = versionNumber.numbers[i];
                }else{
                    otherNumbers[i] = 0;
                }
            }
        }else{
            otherNumbers = versionNumber.numbers;
            localNumbers = numbers;
        }

        for(int i =0; i < otherNumbers.length; i++){
            if(otherNumbers[i] > localNumbers[i]){
                return -1;
            }else if(otherNumbers[i] < localNumbers[i]){
                return 1;
            }
        }

        return 0;
    }

    public boolean into(VersionNumber n){
        for(int i = 0; i < n.numbers.length; i++){
            if(numbers[i] != n.numbers[i]){
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof VersionNumber){
            return compareTo((VersionNumber)obj) == 0;
        }else{
            return false;
        }
    }
}
