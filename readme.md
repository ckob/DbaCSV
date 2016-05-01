#CSVDba
###Funcions
* new Dba()
* new Dba(ruta-arxiu.dat)
  * .from(ruta-arxiu.dat)
  * .selectAll()                -> no necessari, si no s'especifica res, es selecciona tot
  * .select(string1, string2, nom camp)     -> camps a seleccionar en text (no case sensitive)
  * .select(int1, int2,... num camp)        -> camps a seleccionar numericament (no case sensitive)
  * .capcalera(true|false)      -> Indica si volem mostrar la capçalera (els noms dels camps) o no
  * .as(string1, string2... nomAlias)       -> alias a mostrar com a camps
  * .separador(string|char sep) -> a utilitzar com a separdor per a la sortida (per defecte es una tabulació)
  * .orderBy(String|int camp)   -> ordena ASCendentment la sortida aportant el nº/nom de camp (accepta els orderBy's que vulguis)
  * .orderAscBy(String|int camp)-> ordena ASCendentment la sortida aportant el nº/nom de camp (accepta els orderBy's que vulguis)
  * .orderDescBy()              -> ordena DESCendentment la sortida aportant el nº/nom de camp (accepta els orderBy's que vulguis)
  * .where(String|int camp)     -> inicia un where
    * .equals(String|int condicio)    -> evalua la condició utilitzant String. (encara que li passis un int)
    * .equalsIgnoreCase(String condicio)
    * .equalsNum(double condicio)
    * .moreThan(double condicio)
    * .lessThan(double condicio)
    * .between(double condicioInici, double condicioFinal)
  * .toString()                 -> genera un String amb la sortida. (inclou la capçalera (nom camps/alias) si s'ha seleccionat)
  * .toString(char separador)   -> genera un String amb la sortida amb el separador que desitgem
  * .print()                    -> imprimeix per pantalla la sortida de la consulta (sempre es mostra la capçalera (nom camps/alias)
  * .max(String|int Camp)       -> retorna el nombre més gran (en double) del camp indicat al resultat de la consulta
  * .min(String|int Camp)       -> retorna el nombre més petit (en double) del camp indicat al resultat de la consulta
  * .avg(String|int Camp)       -> retorna la mitjana (en double) del camp indicat al resultat de la consulta
  * .sum(String|int Camp)       -> retorna la suma del camp indicat al resultat de la consulta
  * .count()                    -> retorna el nombre de linies que ha generat la consulta
  * .insertInto(ruta-arxiu.dat) -> inicia l'intenció de insertar quelcom. Aquesta acció sobreescriurá l'arxiu per insertar-hi dades!
    * [.camps(String camps)]    -> indica els camps dels valors que introduirem a continuació. Es pot utilitzar en qualsevol ordre.
    * .values(String valors)    -> els valors que volem introduir. En l'ordre de la "base de dades" o en l'ordre especifiat amb .camps().

###Notes
* Les columnes comencen per 0.

####To-do

* .toFile(nom-arxiu.dat)      -> guardar la sortida a un arxiu
* .join(Dba, camp)            -> unir dos Dba's. Donar una sortida igualant el camp indicat de les dues.
* .insert
* Controlar més errors. Afegir excepcions i missatges d'error.