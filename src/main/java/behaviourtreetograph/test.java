package behaviourtreetograph;

import java.util.HashMap;

public class test {

    public static void main(String[] args) {
        String s  = "sdsdsacrttyjn";
        s.toLowerCase();
        HashMap<Character,Integer > map = new HashMap<Character, Integer>();

        for(Character i: s.toCharArray()){
            if (map.keySet().contains(i)){
                int x= map.get(i);
                ++x;
                map.put(i,x);

            }else {
                map.put(i,1);
            }
        }
        System.out.println(map.toString());
    }
}

