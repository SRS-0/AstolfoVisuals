package xyz.angames.astolfoclient.client.util;
import java.util.*;
import com.mojang.blaze3d.platform.InputConstants;
/** Keeps saved keybinds stable while translating SDL input at the boundary. */
public final class LegacyInput {
 private static final Map<Integer,Integer> TO_NATIVE=new HashMap<>(),TO_LEGACY=new HashMap<>();
 static {int[][] codes={{48,39},{49,30},{50,31},{51,32},{52,33},{53,34},{54,35},{55,36},{56,37},{57,38},{65,4},{66,5},{67,6},{68,7},{69,8},{70,9},{71,10},{72,11},{73,12},{74,13},{75,14},{76,15},{77,16},{78,17},{79,18},{80,19},{81,20},{82,21},{83,22},{84,23},{85,24},{86,25},{87,26},{88,27},{89,28},{90,29},{290,58},{291,59},{292,60},{293,61},{294,62},{295,63},{296,64},{297,65},{298,66},{299,67},{300,68},{301,69},{302,104},{303,105},{304,106},{305,107},{306,108},{307,109},{308,110},{309,111},{310,112},{311,113},{312,114},{313,115},{282,83},{320,98},{321,89},{322,90},{323,91},{324,92},{325,93},{326,94},{327,95},{328,96},{329,97},{330,220},{335,88},{336,103},{264,81},{263,80},{262,79},{265,82},{334,87},{39,52},{92,49},{44,54},{61,46},{96,53},{91,47},{45,45},{332,85},{46,55},{93,48},{59,51},{47,56},{32,44},{258,43},{342,226},{341,224},{340,225},{346,230},{345,228},{344,229},{257,40},{256,41},{259,42},{261,76},{269,77},{268,74},{260,73},{267,78},{266,75},{280,57},{284,72},{281,71},{283,70}};for(int[] p:codes){TO_NATIVE.put(p[0],p[1]);TO_LEGACY.put(p[1],p[0]);}}
 public static int nativeKey(int old){return TO_NATIVE.getOrDefault(old,0);}
 public static int legacyKey(int key){return TO_LEGACY.getOrDefault(key,-1);}
 public static int legacyButton(int button){return switch(button){case 1->0;case 3->1;case 2->2;default->button-1;};}
 public static int legacyModifiers(int mask){return ((mask&3)!=0?1:0)|((mask&192)!=0?2:0)|((mask&768)!=0?4:0)|((mask&3072)!=0?8:0);}
 public static int nativeModifiers(int mask){return ((mask&1)!=0?3:0)|((mask&2)!=0?192:0)|((mask&4)!=0?768:0)|((mask&8)!=0?3072:0);}
 public static boolean isKeyDown(int old){return InputConstants.isKeyDown(nativeKey(old));}
}
