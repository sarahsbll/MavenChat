package secure.chat;

import java.sql.*;
import java.util.*;

public class DecryptVote {
    public static String voteD;

    // receive decrypted vote when called by clientlistener
    public static void setVoteD(String lvoteD) {
        voteD = lvoteD;
    }

    // show decrypted vote when called by serevr dispatcher;
    public static void DecryptV() {
        System.out.println("Vote déchiffré");
        System.out.println(voteD);
    }

}