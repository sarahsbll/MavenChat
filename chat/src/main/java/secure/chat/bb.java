package secure.chat;

import java.sql.*;
import java.util.*;

public class bb {
    public static void main(String[] args) {
        /*
         * On a ici 5 wilaya ayant chacune 5 listes electorales (donc 5 partis, chaque
         * parti possède une liste pour chaque wilaya), Nous avons donc 5 liste par
         * wilaya contenant chacune 3 candidats ce qui nous fait un total de 75
         * candidats (5wilaya * 5listes * 3candidat)
         */
        int[] voteListe;
        ArrayList<HashMap[]> popol;

        popol = compteVote();

        System.out.println("Listes electorales pré-Depouillement");
        for (int i = 0; i < 5; i++) {
            System.out.println("Wilaya " + i);
            for (int j = 0; j < 5; j++) {
                System.out.println("Liste  " + j + " | " + popol.get(i)[j]);
            }
            System.out.println("------------------------------");
        }
        System.out.println("||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");

        // You could also decrypt at this point, by writing over the votes with
        // decrypted values after selecing them
        popol = Depouillement();

        System.out.println("Listes electorales post-Depouillement");
        for (int i = 0; i < 5; i++) {
            System.out.println("Wilaya " + i);
            for (int j = 0; j < 5; j++) {
                System.out.println("Liste  " + j + " | " + popol.get(i)[j]);
            }
            System.out.println("------------------------------");
        }
        System.out.println("||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");

        voteListe = UpdateNbvoteListe(popol);
        AssignerSiege(voteListe, popol);
    }

    public static ArrayList<HashMap[]> compteVote() {
        /*
         * Recupere les candidats et les affecte dans la case correspondante a la wilaya
         * et a la liste dans lesquelles il se trouve
         */
        ArrayList<HashMap[]> VotesG = InitStructure();
        final int nbrList = 25;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/dbvote2?autoReconnect=true&useSSL=false", "root", "projetcrypto");
            Statement stmt = con.createStatement();
            ResultSet rs;
            String query;
            int k = 0, j = 0;
            for (int i = 0; i < nbrList; i++) {
                if (k % 5 == 0 & k != 0) {
                    k = 0;
                    j++;
                }
                query = "select * from candidat where NumList =" + i;
                rs = stmt.executeQuery(query);
                while (rs.next()) {
                    VotesG.get(j)[k].put(rs.getInt("CodeCan"), 0);
                }
                k++;
            }
            con.close();
        } catch (Exception e) {
            System.out.println(e);
        }

        return VotesG;
    }

    public static ArrayList<HashMap[]> InitStructure() {
        // Initialiser la structure de donnée qui contient les candidats
        ArrayList<HashMap[]> popol = new ArrayList<>();
        // chaque elt de popol rpz une wilaya
        HashMap[] wilaya0 = new HashMap[5];
        HashMap[] wilaya1 = new HashMap[5];
        HashMap[] wilaya2 = new HashMap[5];
        HashMap[] wilaya3 = new HashMap[5];
        HashMap[] wilaya4 = new HashMap[5];
        // chaque hashmap rpz une liste electorale contenant 3 candidat (numcandidat,
        // nbvote)

        for (int i = 0; i < 5; i++) {
            Arrays.fill(wilaya0, i, i + 1, new HashMap<Integer, Integer>());
            Arrays.fill(wilaya1, i, i + 1, new HashMap<Integer, Integer>());
            Arrays.fill(wilaya2, i, i + 1, new HashMap<Integer, Integer>());
            Arrays.fill(wilaya3, i, i + 1, new HashMap<Integer, Integer>());
            Arrays.fill(wilaya4, i, i + 1, new HashMap<Integer, Integer>());
        }

        popol.add(wilaya0);
        popol.add(wilaya1);
        popol.add(wilaya2);
        popol.add(wilaya3);
        popol.add(wilaya4);
        return popol;
    }

    public static ArrayList<HashMap[]> Depouillement() {
        /* Recuperer chaque vote et le compter pour le candidat correspondant */
        ArrayList<HashMap[]> popol;
        popol = compteVote();
        HashMap<Integer, Integer> map;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/dbvote2?autoReconnect=true&useSSL=false", "root", "projetcrypto");
            Statement stmt = con.createStatement();
            ResultSet rs;
            String query;

            int i, j;
            query = "select * from bulletin";
            rs = stmt.executeQuery(query);
            boolean found;
            while (rs.next()) {
                i = 0;
                j = 0;
                found = false;
                while (!found & i < 5) {
                    while (!found & j < 5) {
                        map = popol.get(i)[j];
                        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {

                            // rs.getBlob (or getVarChar) -> decrypt after shamir with private key

                            if (entry.getKey() == rs.getInt("Vote")) {
                                map.merge(rs.getInt("Vote"), 1, Integer::sum);
                                found = true;
                            }
                        }
                        j++;
                    }
                    j = 0;
                    i++;
                }
            }

            con.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        // update 'NbVoix' de la table candidat
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/dbvote2?autoReconnect=true&useSSL=false", "root", "projetcrypto");
            String query;
            PreparedStatement preparedstmt;
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    map = popol.get(i)[j];
                    for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                        query = "update Candidat2 set NbVoix=? where CodeCan=?";
                        preparedstmt = con.prepareStatement(query);
                        preparedstmt.setInt(1, entry.getValue());
                        preparedstmt.setInt(2, entry.getKey());
                        preparedstmt.executeUpdate();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return popol;
    }

    public static int[] UpdateNbvoteListe(ArrayList<HashMap[]> popol) {
        // update l'attribut Nb vote de la table list
        HashMap<Integer, Integer> map;
        int res = 0, k = 0;
        int[] voteListe = new int[25];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                map = popol.get(i)[j];
                for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                    res = res + entry.getValue();
                }
                voteListe[k] = voteListe[k] + res;
                res = 0;
                k++;
            }
        }
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/dbvote2?autoReconnect=true&useSSL=false", "root", "projetcrypto");
            String query;
            PreparedStatement preparedstmt;
            for (int i = 0; i < 25; i++) {
                query = "update list set NbVote=? where NumList=?";
                preparedstmt = con.prepareStatement(query);
                preparedstmt.setInt(1, voteListe[i]);
                preparedstmt.setInt(2, i);
                preparedstmt.executeUpdate();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return voteListe;
    }

    public static void AssignerSiege(int[] voteliste, ArrayList<HashMap[]> popol) {
        final int sieges = 4;
        int[] siegepris = new int[5];
        int[] coutsiege = new int[5];
        ;
        int[] voteWilaya = new int[5];
        Integer[] resteHare = new Integer[5];
        int[] nbrSiegeParListe = new int[25];
        for (int i = 0; i < voteliste.length; i++) { // Calcul cout siege par wilaya
            switch (i % 5) {
                case 0 -> voteWilaya[0] = voteWilaya[0] + voteliste[i];
                case 1 -> voteWilaya[1] = voteWilaya[1] + voteliste[i];
                case 2 -> voteWilaya[2] = voteWilaya[2] + voteliste[i];
                case 3 -> voteWilaya[3] = voteWilaya[3] + voteliste[i];
                case 4 -> voteWilaya[4] = voteWilaya[4] + voteliste[i];
            }
        }
        for (int i = 0; i < 5; i++) {
            coutsiege[i] = voteWilaya[i] / sieges;
        }

        for (int i = 0; i < 5; i++) {
            switch (i) {
                case 0 -> {
                    for (int k = 0; k < 5; k++) {// assigner des sieges a chaque liste
                        nbrSiegeParListe[k] = voteliste[k] / coutsiege[i];
                        siegepris[i] = siegepris[i] + nbrSiegeParListe[k];
                    }
                    if (siegepris[i] < sieges) {// assigner les sieges restants (Methode du quotient de Hare)
                        for (int k = 0; k < 5; k++) {
                            if (nbrSiegeParListe[k] < 3)
                                resteHare[k] = voteliste[k] - (nbrSiegeParListe[k] * coutsiege[i]);
                            else
                                resteHare[k] = -1;
                        }
                        List<Integer> liste = Arrays.asList(resteHare);
                        int index = liste.indexOf(Collections.max(liste));
                        while (siegepris[i] < sieges) {
                            nbrSiegeParListe[index]++;
                            siegepris[i]++;
                            liste.set(index, -1);
                            index = liste.indexOf(Collections.max(liste));
                        }
                    }
                }
                case 1 -> {
                    for (int k = 5; k < 10; k++) {// assigner des sieges a chaque liste
                        nbrSiegeParListe[k] = voteliste[k] / coutsiege[i];
                        siegepris[i] = siegepris[i] + nbrSiegeParListe[k];
                    }
                    if (siegepris[i] < sieges) {// assigner les sieges restants (Methode du quotient de Hare)
                        for (int k = 5; k < 10; k++) {
                            if (nbrSiegeParListe[k] < 3)
                                resteHare[k - 5] = voteliste[k] - (nbrSiegeParListe[k] * coutsiege[i]);
                            else
                                resteHare[k - 5] = -1;
                        }
                        List<Integer> liste = Arrays.asList(resteHare);
                        int index = liste.indexOf(Collections.max(liste));
                        while (siegepris[i] < sieges) {
                            nbrSiegeParListe[index + 5]++;
                            siegepris[i]++;
                            liste.set(index, -1);
                            index = liste.indexOf(Collections.max(liste));
                        }
                    }
                }
                case 2 -> {
                    for (int k = 10; k < 15; k++) {// assigner des sieges a chaque liste
                        nbrSiegeParListe[k] = voteliste[k] / coutsiege[i];
                        siegepris[i] = siegepris[i] + nbrSiegeParListe[k];
                    }
                    for (int k = 10; k < 15; k++) {
                        nbrSiegeParListe[k] = voteliste[k] / coutsiege[i];
                        siegepris[i] = siegepris[i] + nbrSiegeParListe[k];
                    }
                    if (siegepris[i] < sieges) {// assigner les sieges restants (Methode du quotient de Hare)
                        for (int k = 10; k < 15; k++) {
                            if (nbrSiegeParListe[k] < 3)
                                resteHare[k - 10] = voteliste[k] - (nbrSiegeParListe[k] * coutsiege[i]);
                            else
                                resteHare[k - 10] = -1;
                        }
                        List<Integer> liste = Arrays.asList(resteHare);
                        int index = liste.indexOf(Collections.max(liste));
                        while (siegepris[i] < sieges) {
                            nbrSiegeParListe[index + 10]++;
                            siegepris[i]++;
                            liste.set(index, -1);
                            index = liste.indexOf(Collections.max(liste));
                        }
                    }
                }
                case 3 -> {
                    for (int k = 15; k < 20; k++) {// assigner des sieges a chaque liste
                        nbrSiegeParListe[k] = voteliste[k] / coutsiege[i];
                        siegepris[i] = siegepris[i] + nbrSiegeParListe[k];
                    }
                    if (siegepris[i] < sieges) {// assigner les sieges restants (Methode du quotient de Hare)
                        for (int k = 15; k < 20; k++) {
                            if (nbrSiegeParListe[k] < 3)
                                resteHare[k - 15] = voteliste[k] - (nbrSiegeParListe[k] * coutsiege[i]);
                            else
                                resteHare[k - 15] = -1;
                        }
                        List<Integer> liste = Arrays.asList(resteHare);
                        int index = liste.indexOf(Collections.max(liste));
                        while (siegepris[i] < sieges) {
                            nbrSiegeParListe[index + 15]++;
                            siegepris[i]++;
                            liste.set(index, -1);
                            index = liste.indexOf(Collections.max(liste));
                        }
                    }

                }
                case 4 -> {
                    for (int k = 20; k < 25; k++) {// assigner des sieges a chaque liste
                        nbrSiegeParListe[k] = voteliste[k] / coutsiege[i];
                        siegepris[i] = siegepris[i] + nbrSiegeParListe[k];
                    }
                    if (siegepris[i] < sieges) {// assigner les sieges restants (Methode du quotient de Hare)
                        for (int k = 20; k < 25; k++) {
                            if (nbrSiegeParListe[k] < 3)
                                resteHare[k - 20] = voteliste[k] - (nbrSiegeParListe[k] * coutsiege[i]);
                            else
                                resteHare[k - 20] = -1;
                        }
                        List<Integer> liste = Arrays.asList(resteHare);
                        int index = liste.indexOf(Collections.max(liste));
                        while (siegepris[i] < sieges) {
                            nbrSiegeParListe[index + 20]++;
                            siegepris[i]++;
                            liste.set(index, -1);
                            index = liste.indexOf(Collections.max(liste));
                        }
                    }
                }
            }
        }
        for (int j = 0; j < 25; j++) {
            System.out.println("Le nombre de sieges attribués a la liste : " + j + " est de " + nbrSiegeParListe[j]);
        }
        System.out.println("||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");
        int[] som = new int[5];
        for (int j = 0; j < 25; j++) {
            switch (j % 5) {
                case 0, 1, 2, 3, 4 -> som[j % 5] = som[j % 5] + nbrSiegeParListe[j];
            }
        }
        try { // update la table partiepolitique avec le nombre de sieges que chaque parti a
              // eu
            Class.forName("com.mysql.jdbc.Driver");
            Connection con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/dbvote2?autoReconnect=true&useSSL=false", "root", "projetcrypto");
            ResultSet rs;
            String query;
            PreparedStatement preparedstmt;
            for (int i = 0; i < 5; i++) {
                query = "update partiepolitique2 set NbSiege=? where Codepartie=?";
                preparedstmt = con.prepareStatement(query);
                preparedstmt.setInt(1, som[i]);
                preparedstmt.setInt(2, i);
                preparedstmt.executeUpdate();
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        HashMap<Integer, Integer> map;
        int p;
        for (int j = 0; j < 25; j++) { // mettre l'attribut Siege de la table Candidat a 1 pour chaque candidat ayant
                                       // reçu un siege
            for (int i = 0; i < nbrSiegeParListe[j]; i++) {
                map = popol.get(j / 5)[j % 5];
                p = Collections.max(map.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey();
                System.out.println("Le candidat " + p + " de la liste " + j + " reçoit un siege");
                try { // on assigne un siege aux candidats
                    Class.forName("com.mysql.jdbc.Driver");
                    Connection con = DriverManager.getConnection(
                            "jdbc:mysql://localhost:3306/dbvote2?autoReconnect=true&useSSL=false", "root",
                            "projetcrypto");
                    String query;
                    PreparedStatement preparedstmt;
                    query = "update candidat2 set Siege=1 where CodeCan=?";
                    preparedstmt = con.prepareStatement(query);
                    preparedstmt.setInt(1, p);
                    preparedstmt.executeUpdate();
                } catch (Exception e) {
                    System.out.println(e);
                }
                map.remove(p);
            }
        }

    }

}
