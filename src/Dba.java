import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Dba {

    // Configuració
    private static char separadorMilers = '.';
    private static char separadorDecimals = ',';

    private char separadorEntrada = ',';
    private char possibleDelimitadorCampsEntrada = '\"';

    private char separadorSortida = separadorEntrada;
    private char possibleDelimitadorCampsSortida = possibleDelimitadorCampsEntrada;

    private String tipusNull = "\\N"; // Valor per escriure que un camp es null. (També es pot deixar senzillament sense re)

    private String from = "";

    private int[] select = null;

    private boolean capcalera = true;

    private ArrayList<String[]> consulta;

    private String[] nomsCamps;
    private String[] aliasCamps = null;

    private TIPUS_DADA[] tipusCamps = null;

    private ArrayList<Where> wheres;

    private ArrayList<OrderBy> orderBys;
    private boolean orderByInvertit = false;

    public Dba reset() {
        from = "";
        select = null;
        separadorSortida = ',';
        separadorEntrada = ',';
        capcalera = true;
        consulta = new ArrayList<>();
        wheres = new ArrayList<>();
        orderBys = new ArrayList<>();
        orderByInvertit = false;
        return this;
    }

    public Dba() {
        consulta = new ArrayList<>();
        wheres = new ArrayList<>();
        orderBys = new ArrayList<>();
    }

    public Dba(String from) {
        consulta = new ArrayList<>();
        wheres = new ArrayList<>();
        orderBys = new ArrayList<>();
        this.from = from;
    }

    public Dba from(String ruta) {
        this.from = ruta;
        return this;
    }

    /**
     * S'utilitza per fer un select de tots els camps. No es necessari utilitzarlo si no s'ha fet un select
     * d'algún o alguns camps en concret, ja que si no especifiquem el select, es farà de tots els camps
     * per defecte.
     *
     * @return objecte Dba amb el select a null.
     */
    public Dba selectAll() {
        select = null;
        return this;
    }

    /**
     * Per seleccionar les columnes que volguém. Comença per 0.
     *
     * @param camps a mostrar a la sortida
     * @return objecte Dba amb el select actualitzat
     */
    public Dba select(int... camps) {
        this.select = camps;
        return this;
    }

    /**
     * Fa el select amb el nom de cada camp. Ignora les majúscules i minúscules.
     *
     * @param camps a mostrar a la sortida
     * @return objecte Dba amb el select preparat correctament, amb posicions numeriques.
     */
    public Dba select(String... camps) {
        return select(nomCampsStringToInt(camps));
    }


    /**
     * Indiquem si volem mostrar la capçalera o no. Es a dir, el nom dels camps.
     *
     * @param mostrar true si volem mostrar la capçalera, false en cas contrari
     * @return Objecte Dba amb la capcalera modificada.
     */
    public Dba capcalera(boolean mostrar) {
        capcalera = mostrar;
        return this;
    }

    /**
     * Per personalitzar els noms dels camps de sortida (només visual).
     *
     * @param alias Array de strings amb els alias que volguem mostrar.
     * @return Objecta Dba amb els alias modificats.
     */
    public Dba as(String... alias) {
        aliasCamps = alias;
        return this;
    }

    /**
     * Modifiquem el separadorSortida per defecte.
     *
     * @param separador Separador per a la sortida per String.
     * @return Objecte Dba amb el separadorSortida modificat.
     */
    public Dba separadorSortida(char separador) {
        this.separadorSortida = separador;
        return this;
    }

    /**
     *
     * @param fila un string separat amb estil csv
     * @return array de Strings amb un valor per cada camp separat correctament.
     */
    private String[] filaCsvToArrString(String fila) {
        ArrayList<String> strings = new ArrayList<>();
        String str = "";
        boolean entreCometes = false;
        for (int i = 0; i < fila.length(); i++) {
            if (fila.charAt(i) == possibleDelimitadorCampsEntrada) {
                if (i + 1 < fila.length()) {
                    if ((fila.charAt(i + 1) == possibleDelimitadorCampsEntrada)) { // Escapar cometes
                        str += fila.charAt(i); // Afegeixo unes cometes de les dues que hi han.
                        i++;                    // i ja no cal mirar les següents
                    } else {
                        entreCometes = !entreCometes;
                    }
                    continue;
                } else if (i==fila.length()-1) { // evitar l'ultim possible delimitador de la linia
                    break;
                }
            }
            if (fila.charAt(i) == separadorEntrada && !entreCometes) {
                strings.add(str);
                str = "";
            } else {
                str += fila.charAt(i);
            }
        }
        strings.add(str); // ultim camp
        return strings.toArray(new String[strings.size()]);
    }


    /**
     * Execució de la consulta. Es on es realitza tot el treball, condicionat per els parámetres anteriorment aplicats.
     *
     * @return true si la consulta s'ha realitzat amb éxit
     */
    private boolean treball() {
        consulta.clear();
        BufferedReader br = null;
        try {
            String actualLine;
            br = new BufferedReader(new FileReader(from));
            nomsCamps = filaCsvToArrString(br.readLine());
            tipusCamps = new TIPUS_DADA[nomsCamps.length];
            while ((actualLine = br.readLine()) != null) {
                String[] linia = filaCsvToArrString(actualLine);
                for (int i = 0; i < linia.length; i++) { // Per comprovar el tipus de dades que té cada camp
                    if (tipusCamps[i] != TIPUS_DADA.TEXT) { // TODO: 17/04/16 AFEGIR TIPUS DATA (FECHA) ? (per ordenar no cal, ja que el format yyyy-mm-dd permet fer-ho per string)
                        if (esNumeric(linia[i]) || linia[i].equals(tipusNull) || linia[i].isEmpty()) {
                            tipusCamps[i] = TIPUS_DADA.NUMERIC;
                        } else {
                            tipusCamps[i] = TIPUS_DADA.TEXT;
                        }
                    }
                }
                consulta.add(linia);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // return false;
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        if (!wheres.isEmpty()) {
            ArrayList<String[]> aBorrar = new ArrayList<>();
            for (Where w : wheres) {
                for (String[] strings : consulta) {
                    if (w.type == TIPUS_WHERE.EQUALS) {
                        if (!strings[w.camp].equals(w.condicio)) {
                            aBorrar.add(strings);
                        }
                    }

                    if (w.type == TIPUS_WHERE.EQUALS_IGNORECASE) {
                        if (!strings[w.camp].equalsIgnoreCase(w.condicio)) {
                            aBorrar.add(strings);
                        }
                    }

                    if (w.type == TIPUS_WHERE.EQUALS_NUMERIC) {
                        if (tipusCamps[w.camp] == TIPUS_DADA.NUMERIC) {
                            if (strings[w.camp].isEmpty() || strings[w.camp].equals(tipusNull)) { // Si es null, el trec de la llista
                                aBorrar.add(strings);
                            } else {
                                double aComparar = toDouble(strings[w.camp]);
                                if (aComparar == w.condicioDouble) {
                                    aBorrar.add(strings);
                                }
                            }
                        } else {
                            System.err.println("Error: el camp " + nomsCamps[w.camp] + " no es numeric");
                            return false;
                        }
                    }
                    if (w.type == TIPUS_WHERE.MORE_THAN) {
                        if (tipusCamps[w.camp] == TIPUS_DADA.NUMERIC) {
                            if (strings[w.camp].isEmpty() || strings[w.camp].equals(tipusNull)) { // Si es null, el trec de la llista
                                aBorrar.add(strings);
                            } else {
                                double aComparar = toDouble(strings[w.camp]);
                                if (aComparar <= w.condicioDouble) {
                                    aBorrar.add(strings);
                                }
                            }
                        } else {
                            System.err.println("Error: el camp " + nomsCamps[w.camp] + " no es numeric");
                            return false;
                        }
                    }
                    if (w.type == TIPUS_WHERE.LESS_THAN) {
                        if (tipusCamps[w.camp] == TIPUS_DADA.NUMERIC) {
                            if (strings[w.camp].isEmpty() || strings[w.camp].equals(tipusNull)) { // Si es null, el trec de la llista
                                aBorrar.add(strings);
                            } else {
                                double aComparar = toDouble(strings[w.camp]);
                                if (aComparar >= w.condicioDouble) {
                                    aBorrar.add(strings);
                                }
                            }
                        } else {
                            System.err.println("Error: el camp " + nomsCamps[w.camp] + " no es numeric");
                            return false;
                        }
                    }
                }
            }
            for (String[] strings : aBorrar) {
                consulta.remove(strings);
            }
        }
        if (!orderBys.isEmpty()) {
            if (!orderByInvertit) {
                Collections.reverse(orderBys); // invers ja que vull que tingui més pes el que s'ha afegit abans (l'ultim que es faci, serà el "definitiu")
                orderByInvertit = true;
            }
            for (OrderBy orderBy : orderBys) {
                ordenar(orderBy);
            }
        }

        if (select == null) { // Si el select es null, vol dir que seleccioan tots els camps
            select = new int[nomsCamps.length];
            for (int i = 0; i < nomsCamps.length; i++) {
                select[i] = i;
            }
        }
        return true;
    }

    /**
     * Comprovem si un camp (en string) es numèric en la seva totalitat
     *
     * @param str possible número dins d'un string
     * @return true si es numeric, fals si no ho és
     */
    private boolean esNumeric(String str) {
        return str.matches("-?("+separadorMilers+"?\\d+)+("+separadorDecimals+"\\d+)?");
    }

    /**
     *
     * @param strNumeric String a convertir a double
     * @return el número en format double
     */
    private static double toDouble(String strNumeric) {
        strNumeric = strNumeric.replaceAll("\\"+String.valueOf(separadorMilers), "");
        return Double.parseDouble(strNumeric.replace(separadorDecimals, '.'));
    }

    /**
     *
     * @param strNumeric String a convertir a int
     * @return el número en format int
     */
    private int toInt(String strNumeric) {
        return Integer.parseInt(strNumeric.replaceAll("\\"+String.valueOf(separadorMilers), ""));
    }

    /**
     * @return la consulta en forma de string
     */
    @Override
    public String toString() {
        if (treball()) {
            String str = "";
            if (capcalera) {
                String[] aliasCampsAmbNulls;
                if (aliasCamps != null) {
                    aliasCampsAmbNulls = new String[nomsCamps.length];
                    for (int i = 0; i < aliasCamps.length; i++) {
                        aliasCampsAmbNulls[i] = aliasCamps[i];
                    }
                } else {
                    aliasCampsAmbNulls = null;
                }
                int aux = 0;
                for (int i : select) {
                    if (aliasCampsAmbNulls == null || aliasCampsAmbNulls[aux] == null) {
                        str += nomsCamps[i] + separadorSortida;
                    } else {
                        str += aliasCampsAmbNulls[aux] + separadorSortida;
                    }
                    aux++;
                }
                str = str.substring(0, str.length() - 1) + "\n"; // Borro l'ultim separador de sortida. (1 caracter de longitud)
            }
            for (String[] fila : consulta) {
                for (int i : select) {
                    str += fila[i] + separadorSortida;
                }
                str = str.substring(0, str.length() - 1) + "\n";
            }
            return str.substring(0, str.length() - 1);
        } else {
            return "S'ha produït un error.";
        }
    }

    public String toString(char separador) {
        separadorSortida=separador;
        return toString();
    }

    /**
     * Imprimeix per pantalla la sortida en una quadricula.
     * Sempre mostra els noms dels camps o alias si s'han ficat.
     */
    public void print() {
        if (treball()) {
            String[] aliasCampsAmbNulls;
            if (aliasCamps != null) {
                aliasCampsAmbNulls = new String[nomsCamps.length];
                for (int i = 0; i < aliasCamps.length; i++) {
                    aliasCampsAmbNulls[i] = aliasCamps[i];
                }
            } else {
                aliasCampsAmbNulls = null;
            }

            int[] maxLengthCamps = new int[nomsCamps.length];
            int aux = 0;
            for (int i : select) {
                if (aliasCampsAmbNulls == null || aliasCampsAmbNulls[aux] == null) {
                    maxLengthCamps[i] = nomsCamps[i].length();
                } else {
                    maxLengthCamps[i] = aliasCampsAmbNulls[aux].length();
                }
                aux++;
            }

            for (String[] linia : consulta) {
                for (int i : select) {
                    if (linia[i].length() > maxLengthCamps[i]) {
                        maxLengthCamps[i] = linia[i].length();
                    }
                }
            }
            String filaSeparadora = "";
            for (int i : select) {
                filaSeparadora += "+";
                if (maxLengthCamps[i] > 0) {
                    for (int j = 0; j < maxLengthCamps[i]; j++) {
                        filaSeparadora += "-";
                    }
                }
            }
            filaSeparadora += "+";

            String str = "" + filaSeparadora + "\n" + "|";
            aux = 0;
            for (int i : select) {
                if (aliasCampsAmbNulls == null || aliasCampsAmbNulls[aux] == null) {
                    str += encaixarStrAMida(nomsCamps[i], maxLengthCamps[i], TIPUS_JUSTIFICACIO.CENTRAT) + "|";
                } else {
                    str += encaixarStrAMida(aliasCampsAmbNulls[aux], maxLengthCamps[i], TIPUS_JUSTIFICACIO.CENTRAT) + "|";
                }
                aux++;
            }

            // Fila única per afegir a sota dels noms/alias dels camps. Per poder indicar si s'ha ordenat per algun camp:
            if (!orderBys.isEmpty()) {
                String filaUnica = "";
                OrderBy[] arrOrderBy = new OrderBy[nomsCamps.length];
                for (OrderBy orderBy : orderBys) {
                    arrOrderBy[orderBy.camp] = orderBy;
                }
                for (int i : select) {
                    filaUnica += "+";
                    if (maxLengthCamps[i] > 0) {
                        if (arrOrderBy[i] != null) {
                            for (int j = 0; j < maxLengthCamps[i]; j++) {
                                if ((maxLengthCamps[i]) / 2 == j) {
                                    if (arrOrderBy[i].asc) {
//                                                filaUnica+="/\\";
                                        filaUnica += "△";
                                        j++;
                                    } else {
//                                                filaUnica+="\\/";
                                        filaUnica += "▽";
                                        j++;
                                    }
                                }
                                filaUnica += "-";
                            }
                        } else {
                            for (int j = 0; j < maxLengthCamps[i]; j++) {
                                filaUnica += "-";
                            }
                        }
                    }
                }
                filaUnica += "+";
                str += "\n" + filaUnica + "\n";
            } else {
                str += "\n" + filaSeparadora + "\n";
            }

            for (String[] fila : consulta) {
                str += "|";
                for (int i : select) {
                    if (tipusCamps[i] == TIPUS_DADA.NUMERIC) {
                        str += encaixarStrAMida(fila[i], maxLengthCamps[i], TIPUS_JUSTIFICACIO.DRETA) + "|";
                    } else {
                        str += encaixarStrAMida(fila[i], maxLengthCamps[i], TIPUS_JUSTIFICACIO.ESQUERRA) + "|";
                    }
                }
                //str += "\n" + filaSeparadora + "\n"; // Per separar molt més les files entre ells
                str += "\n";
            }
            //str +=  filaSeparadora + "\n";
            str += "("+count()+" rows)";
            System.out.println(str);
        } else {
            System.out.println("S'ha produït un error.");
        }
    }

    /**
     * pasat un string, la mida que volem que sigui i on el volem situar, el retorna formatejat
     * @param str el text
     * @param mida la mida que volem obtenir
     * @param JUST el tipus de justificació (esquerra, dreta o centrat)
     * @return el string encaixar on volem i amb la vida que volem, omplint la resta amb esapis
     */
    private String encaixarStrAMida(String str, int mida, TIPUS_JUSTIFICACIO JUST) {
        if (JUST == TIPUS_JUSTIFICACIO.ESQUERRA) {
            for (int i = str.length(); i < mida; i++)
                str += " ";
        } else if (JUST == TIPUS_JUSTIFICACIO.DRETA) {
            for (int i = str.length(); i < mida; i++)
                str = " " + str;
        } else if (JUST == TIPUS_JUSTIFICACIO.CENTRAT) {
            for (int i = str.length(); i < mida; i++) {
                if (i % 2 == 0)
                    str += " ";
                else
                    str = " " + str;
            }
        }
        return str;
    }

    /**
     * Conta la cantitat de linies que ha obtingut la consulta
     *
     * @return la cantitat de linies que ha obtingut la consulta
     */
    public int count() {
        if (consulta != null && consulta.isEmpty()) {
            if (treball()) {
                return consulta.size();
            } else {
                System.err.println("S'ha produït un error.");
                return -1;
            }
        } else {
            if (consulta != null)
                return consulta.size();
            else
                return -1;
        }

    }

    /**
     * Funció interna per reutilitzar codi. Es pasa com a argument el camp a obtenir i si volem obtenir el maxim o el minim
     *
     * @param camp el camp a buscar
     * @param max  true si volem obtenir el valor máxim. Fals si volem el mínim.
     * @return el nombre més gran o més petit del camp indicat al resultat de la consulta
     */
    private double internCalcLimit(int camp, boolean max) {
        double tmp = max ? Double.MIN_VALUE : Double.MAX_VALUE;
        double actual;
        for (String[] linia : consulta) {
            if (esNumeric(linia[camp])) {
                actual = toDouble(linia[camp]);
                if (max) {
                    if (actual > tmp)
                        tmp = actual;
                } else {
                    if (actual < tmp)
                        tmp = actual;
                }
            } else {
                System.err.println("Error: el camp " + nomsCamps[camp] + " no es numeric");
                return max ? Double.MIN_VALUE : Double.MAX_VALUE;
            }

        }
        return tmp;
    }

    /**
     * @param camp el camp en número. Comença per 0
     * @return el nombre més gran del camp indicat al resultat de la consulta
     */
    public double max(int camp) {

        if (consulta != null && consulta.isEmpty()) {
            if (treball()) {
                return internCalcLimit(camp, true);
            } else {
                System.err.println("S'ha produït un error.");
                return Double.MIN_VALUE;
            }
        } else {
            if (consulta != null) {
                return internCalcLimit(camp, true);
            } else
                return Double.MIN_VALUE;
        }
    }

    /**
     * @param camp el camp, en text.
     * @return el nombre més gran del camp indicat al resultat de la consulta
     */
    public double max(String camp) {
        return max(nomCampsStringToInt(camp)[0]);
    }

    /**
     * @param camp el camp en número. Comença per 0
     * @return el nombre més petit del camp indicat al resultat de la consulta
     */
    public double min(int camp) {

        if (consulta != null && consulta.isEmpty()) {
            if (treball()) {
                return internCalcLimit(camp, false);
            } else {
                System.err.println("S'ha produït un error.");
                return Double.MAX_VALUE;
            }
        } else {
            if (consulta != null) {
                return internCalcLimit(camp, false);
            } else
                return Double.MAX_VALUE;
        }
    }

    /**
     * @param camp el camp, en text.
     * @return el nombre més gran del camp indicat al resultat de la consulta
     */
    public double min(String camp) {
        return min(nomCampsStringToInt(camp)[0]);
    }

    /**
     * @param camp el camp en número. Comença per 0
     * @return la mitjana del camp indicat al resultat de la consulta
     */
    public double avg(int camp) {

        if (consulta != null && consulta.isEmpty()) {
            if (treball()) {
                double total = 0;
                for (String[] linia : consulta) {
                    if (esNumeric(linia[camp])) {
                        total += toDouble(linia[camp]);
                    } else {
                        System.err.println("Error: el camp " + nomsCamps[camp] + " no es numeric");
                        return 0;
                    }
                }
                return total / consulta.size();
            } else {
                System.err.println("S'ha produït un error.");
                return 0;
            }
        } else {
            if (consulta != null) {
                double total = 0;
                for (String[] linia : consulta) {
                    if (esNumeric(linia[camp])) {
                        total += toDouble(linia[camp]);
                    } else {
                        System.err.println("Error: el camp " + nomsCamps[camp] + " no es numeric");
                        return 0;
                    }
                }
                return total / consulta.size();
            } else
                return 0;
        }
    }

    /**
     * @param camp el camp, en text.
     * @return el nombre més gran del camp indicat al resultat de la consulta
     */
    public double avg(String camp) {
        return avg(nomCampsStringToInt(camp)[0]);
    }

    /**
     * @param camp el camp en número. Comença per 0
     * @return la suma de tots els camps al resultat de la consulta
     */
    public double sum(int camp) {

        if (consulta != null && consulta.isEmpty()) {
            if (treball()) {
                double total = 0;
                for (String[] linia : consulta) {
                    if (esNumeric(linia[camp])) {
                        total += toDouble(linia[camp]);
                    } else {
                        System.err.println("Error: el camp " + nomsCamps[camp] + " no es numeric");
                        return 0;
                    }
                }
                return total;
            } else {
                System.err.println("S'ha produït un error.");
                return 0;
            }
        } else {
            if (consulta != null) {
                double total = 0;
                for (String[] linia : consulta) {
                    if (esNumeric(linia[camp])) {
                        total += toDouble(linia[camp]);
                    } else {
                        System.err.println("Error: el camp " + nomsCamps[camp] + " no es numeric");
                        return 0;
                    }
                }
                return total;
            } else
                return 0;
        }
    }

    /**
     * @param camp el camp, en text.
     * @return la suma de tots els camps al resultat de la consulta
     */
    public double sum(String camp) {
        return sum(nomCampsStringToInt(camp)[0]);
    }

    /**
     * @param orderBy objecte OrderBy que conté el camp a ordenar i si ha de ser ascendent o descendentment
     * @return ordena la consulta i retorna true si s'ha pogut ordenar. (per ara, sempre)
     */
    private boolean ordenar(final OrderBy orderBy) { // Ordena la taula a partir de la fila donada (string numeric)
        final int camp = orderBy.camp; // Obliga a ser final

        Collections.sort(consulta, new Comparator<String[]>() {
            @Override
            public int compare(String[] str1, String[] str2) {
                if (str1[camp].isEmpty() || str1[camp].equals(tipusNull))
                    return -1;
//                    return Integer.MIN_VALUE;
                if (str2[camp].isEmpty() || str2[camp].equals(tipusNull))
                    return 1;
//                    return Integer.MAX_VALUE;
                if (tipusCamps[camp] == TIPUS_DADA.NUMERIC)
                    return (int)(toDouble(str1[camp]) - toDouble(str2[camp])); // per numeric
                else // if (tipusCamps[camp] == TIPUS_DADA.TEXT)
                    return str1[camp].compareTo(str2[camp]); // per Strings
            }
        });
        if (!orderBy.asc)
            Collections.reverse(consulta);
        return true;
    }


    /**
     * S'utilitza buscar a l'arxiu els noms dels camps i transformar-los a numeric per poder trebalar amb ells
     *
     * @param select noms dels camps en String per passar-los a Int
     * @return array de int's amb les posicions (columnes) que necessitem
     */
    private int[] nomCampsStringToInt(String... select) {
        BufferedReader br = null;
        int[] newSelect = new int[select.length];
        try {
            br = new BufferedReader(new FileReader(from));
            //String[] camps = br.readLine().toUpperCase().split("\\s+");
            String[] camps = filaCsvToArrString(br.readLine().toUpperCase());
            for (int i = 0; i < select.length; i++) {
                newSelect[i] = java.util.Arrays.asList(camps).indexOf(select[i].toUpperCase());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return newSelect;
    }




    public class Where {
        private Dba dba;
        private int camp;
        private TIPUS_WHERE type;
        private String condicio;
        private Double condicioDouble;

        private Where(Dba dba, int camp) {
            this.dba = dba;
            this.camp = camp;
            condicio = null;
            type = null;
            dba.wheres.add(this);
        }

        public Dba equals(int condicio) {
            type = TIPUS_WHERE.EQUALS;
            this.condicio = String.valueOf(condicio);
            return dba;
        }

        public Dba equalsNum(double condicio) {
            type = TIPUS_WHERE.EQUALS_NUMERIC;
            this.condicioDouble = condicio;
            return dba;
        }

        public Dba equals(String condicio) {
            type = TIPUS_WHERE.EQUALS;
            this.condicio = condicio;
            return dba;
        }

        public Dba equalsIgnoreCase(String condicio) {
            type = TIPUS_WHERE.EQUALS_IGNORECASE;
            this.condicio = condicio;
            return dba;
        }

        public Dba moreThan(double condicio) {
            type = TIPUS_WHERE.MORE_THAN;
            condicioDouble = condicio;
            return dba;
        }

        public Dba lessThan(double condicio) {
            type = TIPUS_WHERE.LESS_THAN;
            condicioDouble = condicio;
            return dba;
        }

        public Dba between(double condicioInici, double condicioFinal) {
            new Where(dba, camp).moreThan(condicioInici);
            type = TIPUS_WHERE.LESS_THAN;
            condicioDouble = condicioFinal;
            return dba;
        }
    }

    public Where where(int camp) {
        return new Where(this, camp);
    }

    public Where where(String camp) {
        return where(nomCampsStringToInt(camp)[0]);
    }

    /**
     * Ordena la consulta ascendentment per el camp (int) indicat
     *
     * @param camp a ordenar
     * @return objecte Dba actualitzat.
     */
    public Dba orderAscBy(int camp) {
        orderBys.add(new OrderBy(camp, true));
        return this;
    }

    /**
     * Ordena la consulta ascendentment per el camp (String) indicat
     *
     * @param camp a ordenar
     * @return objecte Dba actualitzat.
     */
    public Dba orderAscBy(String camp) {
        return orderAscBy(nomCampsStringToInt(camp)[0]);
    }

    /**
     * Ordena per defecte ascendentment
     *
     * @param camp a ordenar
     * @return objecte Dba actualitzat.
     */
    public Dba orderBy(String camp) {
        return orderAscBy(nomCampsStringToInt(camp)[0]);
    }

    /**
     * Ordena la consulta descententment per el camp (int) indicat
     *
     * @param camp a ordenar
     * @return objecte Dba actualitzat.
     */
    public Dba orderDescBy(int camp) {
        orderBys.add(new OrderBy(camp, false));
        return this;
    }

    /**
     * Ordena la consulta descententment per el camp (String) indicat
     *
     * @param camp a ordenar
     * @return objecte Dba actualitzat.
     */
    public Dba orderDescBy(String camp) {
        return orderDescBy(nomCampsStringToInt(camp)[0]);
    }

    /**
     * Clase per crear l'objecte OrderBy, on es guarda el camp per el qual es vol ordenar i si es vol fer ascendent o descendentment
     */
    private class OrderBy {
        private int camp;
        private boolean asc;

        private OrderBy(int camp, boolean asc) {
            this.camp = camp;
            this.asc = asc;
        }
    }

    /**
     * Guarda els resultats d'una consulta a un arxiu, en format csv
     * @param arxiu on es guardarà la consulta
     * @return true si tot ha anat bé
     */
    public boolean toFile(String arxiu) {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(arxiu)));
            pw.println(filaArrCsvToString(nomsCamps)); // imprimeixo els noms dels camps a l'arxiu
            for (String[] fila : consulta) {
                pw.println(filaArrCsvToString(fila));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pw!=null) {
                try {
                    pw.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    /**
     * Converteix una fila d'array d'strings en una fila en fomat CSV String
     * @param fila en format array d'strings
     * @return el String en format csv
     */
    private String filaArrCsvToString (String[] fila) {
        String str="";
        for (String s : fila) {
            if (s!=null) {
                if (!s.isEmpty()) {
                    if (s.indexOf(separadorSortida) >= 0 || s.indexOf(possibleDelimitadorCampsSortida) >= 0) { // Si hi ha quelcom que pugui donar problemes ...
                        s = s.replace(String.valueOf(possibleDelimitadorCampsSortida), String.valueOf(possibleDelimitadorCampsSortida) + String.valueOf(possibleDelimitadorCampsSortida)); // Duplico les, segurament, cometes dobles
                        s = possibleDelimitadorCampsSortida + s + possibleDelimitadorCampsSortida;
                    }
                }
            } else {
                s="";
            }
            str+=s+separadorSortida;
        }
        return str.substring(0, str.length()-1); // trec l'ultima coma
    }

    /**
     * Métode intern per a realitzar un insert
     * @param insertObj objecte Insert que conte els valors a insertar i on han d'anar
     * @return true si tot ha anat bé
     */
    private boolean doInsert(Insert insertObj) {
        if (treball()) {
            if (insertObj.camps==null) { // S'han introduït els valors directament, sense especificar a on van,
                if(insertObj.valors.length == nomsCamps.length) { // En cas de que el nombre de valors introudits sigui el correcte...
                    consulta.add(insertObj.valors);
                } else {
                    System.err.println("Nombre de camps incorrecte. Han d'haver-hi "+nomsCamps.length+" camps. O bé utilitzar .camps(nomsCamps) abans per especificar quins camps vols introduïr.");
                    return false;
                }
            } else {
                if (insertObj.camps.length == insertObj.valors.length) {
                    String[] tmp = new String[nomsCamps.length];
                    int[] numCamps = nomCampsStringToInt(insertObj.camps);
                    for (int i = 0; i < numCamps.length; i++) {
                        tmp[numCamps[i]] = insertObj.valors[i];
                    }
                    consulta.add(tmp);
                } else {
                    System.err.println("Nombre de camps o de valors incorrecte. Han d'haver-hi la mateixa cantitat de camps que de valors.");
                    return false;
                }
            }

            if (toFile(from)) {
                System.out.println("INSERT 0 1");
                return true;
            } else {
                return false;
            }
        } else {
            System.err.println("S'ha produit un error");
            return false;
        }
    }

    public class Insert {
        private String fitxer;
        private String[] valors;
        private String[] camps;

        private Insert(Dba dba, String fitxer) {
            dba.reset();
            from=fitxer;
            this.camps=null;
        }
        public Insert camps(String... camps) {
            this.camps=camps;
            return this;
        }
        public Insert values(String... valors) {
            this.valors=valors;
            doInsert(this);
            return this;
            //return this.dba;
        }
    }
    public Insert insertInto(String fitxer) {
        return new Insert(this, fitxer);
    }



    /**
     * enum per organitzar els tipus de dades
     */
    private enum TIPUS_DADA {
        TEXT, NUMERIC
    }

    /**
     * enum per organitzar els tipus de where's
     */
    private enum TIPUS_WHERE {
        EQUALS, EQUALS_NUMERIC, EQUALS_IGNORECASE, MORE_THAN, LESS_THAN
    }

    /**
     * enum per organitzar els tipus de justificacions a l'hora d'imprimir la taula
     */
    private enum TIPUS_JUSTIFICACIO {
        CENTRAT, ESQUERRA, DRETA
    }
}