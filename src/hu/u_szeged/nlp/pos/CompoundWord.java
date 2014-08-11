package hu.u_szeged.nlp.pos;

import hu.u_szeged.nlp.pos.KRUtils.KRPOS;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * összetett szavak elemzése
 * 
 * @author zsjanos
 * 
 */
public class CompoundWord {

  public static boolean isCompatibleAnalyises(String firstPartKR, String secondPartKR) {

    KRPOS secondPartPOS = KRUtils.getPOS(secondPartKR);
    KRPOS firstPartPOS = KRUtils.getPOS(secondPartKR);

    // UTT-INT nem lehet a második rész
    if (secondPartPOS.equals(KRPOS.UTT_INT)) {
      return false;
    }

    // ART nem lehet a második rész
    if (secondPartPOS.equals(KRPOS.ART)) {
      return false;
    }

    // NUM előtt csak NUM állhat
    if (secondPartPOS.equals(KRPOS.NUM) && !firstPartPOS.equals(KRPOS.NUM)) {
      return false;
    }

    // PREV nem lehet a második rész
    if (secondPartPOS.equals(KRPOS.PREV)) {
      return false;
    }

    // NOUN + ADV letiltva
    if (firstPartPOS.equals(KRPOS.NOUN) && secondPartPOS.equals(KRPOS.ADV)) {
      return false;
    }

    // VERB + ADV letiltva
    if (firstPartPOS.equals(KRPOS.VERB) && secondPartPOS.equals(KRPOS.ADV)) {
      return false;
    }

    // PREV + NOUN letiltva
    if (firstPartPOS.equals(KRPOS.PREV) && secondPartPOS.equals(KRPOS.NOUN)) {
      return false;
    }

    // ADJ + VERB letiltva
    if (firstPartPOS.equals(KRPOS.ADJ) && secondPartPOS.equals(KRPOS.VERB)) {
      return false;
    }

    // VERB + NOUN letiltva
    if (firstPartPOS.equals(KRPOS.VERB) && secondPartPOS.equals(KRPOS.NOUN)) {
      return false;
    }

    // NOUN + VERB csak akkor lehet, ha van a NOUN-nak <CAS>
    if (firstPartPOS.equals(KRPOS.NOUN) && secondPartPOS.equals(KRPOS.VERB) && !firstPartKR.contains("CAS")) {
      return false;
    }

    // NOUN + VERB<PAST><DEF> és nincs a NOUN-nak <CAS> akkor /ADJ
    if (firstPartPOS.equals(KRPOS.NOUN) && secondPartPOS.equals(KRPOS.VERB) && !firstPartKR.contains("CAS") && secondPartKR.contains("<PAST><DEF>")
        && secondPartKR.contains("<DEF>")) {
      return false;
    }

    return true;
  }

  public static boolean isBisectable(String compoundWord) {
    for (int i = 2; i < compoundWord.length() - 1; ++i) {
      if (MagyarlancResourceHolder.getRFSA().analyse(compoundWord.substring(0, i)).size() > 0
          && MagyarlancResourceHolder.getRFSA().analyse(compoundWord.substring(i, compoundWord.length())).size() > 0) {
        return true;
      }
    }

    return false;
  }

  public static int bisectIndex(String compoundWord) {
    for (int i = 2; i < compoundWord.length() - 1; ++i) {
      if (MagyarlancResourceHolder.getRFSA().analyse(compoundWord.substring(0, i)).size() > 0
          && MagyarlancResourceHolder.getRFSA().analyse(compoundWord.substring(i, compoundWord.length())).size() > 0) {
        return i;
      }
    }

    return 0;
  }

  public static LinkedHashSet<String> getCompatibleAnalises(String firstPart, String secondPart) {
    return getCompatibleAnalises(firstPart, secondPart, false);
  }

  public static LinkedHashSet<String> getCompatibleAnalises(String firstPart, String secondPart, boolean hyphenic) {
    LinkedHashSet<String> analises = null;
    analises = new LinkedHashSet<String>();

    Collection<String> firstAnalises = null;
    Collection<String> secondAnalises = null;

    firstAnalises = MagyarlancResourceHolder.getRFSA().analyse(firstPart);
    secondAnalises = MagyarlancResourceHolder.getRFSA().analyse(secondPart);

    String firstPartKR = null;
    String secondPartKR = null;

    if (firstAnalises.size() > 0 && secondAnalises.size() > 0) {
      for (String f : firstAnalises) {
        for (String s : secondAnalises) {

          firstPartKR = KRUtils.getRoot(f);
          secondPartKR = KRUtils.getRoot(s);

          if (isCompatibleAnalyises(firstPartKR, secondPartKR)) {
            if (hyphenic) {
              analises.add(secondPartKR.replace("$", "$" + firstPart + "-"));
            } else {
              analises.add(secondPartKR.replace("$", "$" + firstPart));
            }
          }
        }
      }
    }

    return analises;
  }

  public static LinkedHashSet<String> analyseCompoundWord(String compoundWord) {

    LinkedHashSet<String> analises = null;
    analises = new LinkedHashSet<String>();

    String firstPart = null;
    String secondPart = null;

    int bisectIndex = 0;

    // 2 részre vágható van elemzés
    if (isBisectable(compoundWord)) {

      bisectIndex = bisectIndex(compoundWord);
      firstPart = compoundWord.substring(0, bisectIndex);
      secondPart = compoundWord.substring(bisectIndex, compoundWord.length());
      analises = getCompatibleAnalises(firstPart, secondPart);

    }

    // ha nem bontható 2 részre
    else {
      for (int i = 2; i < compoundWord.length() - 1; ++i) {
        firstPart = compoundWord.substring(0, i);
        secondPart = compoundWord.substring(i, compoundWord.length());

        Collection<String> firstPartAnalises = null;
        firstPartAnalises = MagyarlancResourceHolder.getRFSA().analyse(firstPart);
        if (firstPartAnalises.size() > 0) {
          // ha a második rész két részre bontható
          if (isBisectable(secondPart)) {
            LinkedHashSet<String> secondPartAnalises = null;
            String firstPartOfSecondSection = null;
            String secondPartOfSecondSection = null;
            bisectIndex = bisectIndex(secondPart);
            firstPartOfSecondSection = secondPart.substring(0, bisectIndex);
            secondPartOfSecondSection = secondPart.substring(bisectIndex, secondPart.length());
            secondPartAnalises = getCompatibleAnalises(firstPartOfSecondSection, secondPartOfSecondSection);

            for (String firstAnalyse : firstPartAnalises) {
              for (String secondAnalyse : secondPartAnalises) {
                if (isCompatibleAnalyises(KRUtils.getRoot(firstAnalyse), KRUtils.getRoot(secondAnalyse))) {
                  analises.add(KRUtils.getRoot(secondAnalyse).replace("$", "$" + firstPart));

                }
              }
            }
          }
        }
      }
    }
    return analises;
  }

}