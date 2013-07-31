/*
 * $Id$
 */

package edu.jas.application;


import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import edu.jas.gb.SolvableGroebnerBaseAbstract;
import edu.jas.gb.SolvableGroebnerBaseSeq;
import edu.jas.gbmod.SolvableSyzygyAbstract;
import edu.jas.kern.StringUtil;
import edu.jas.poly.GenSolvablePolynomial;
import edu.jas.poly.GenSolvablePolynomialRing;
import edu.jas.poly.PolynomialList;
import edu.jas.structure.GcdRingElem;
import edu.jas.structure.RingFactory;


// import edu.jas.ufd.GreatestCommonDivisor;
// import edu.jas.ufd.GCDFactory;

/**
 * SolvableLocal ring class based on GenSolvablePolynomial with RingElem
 * interface. Objects of this class are effective immutable.
 * @author Heinz Kredel
 */
public class SolvableLocalRing<C extends GcdRingElem<C>> implements RingFactory<SolvableLocal<C>> {


    private static final Logger logger = Logger.getLogger(SolvableLocalRing.class);


    private final boolean debug = logger.isDebugEnabled();


    /**
     * Solvable polynomial ideal for localization.
     */
    public final SolvableIdeal<C> ideal;


    /**
     * Solvable polynomial ring of the factory.
     */
    public final GenSolvablePolynomialRing<C> ring;


    /**
     * Syzygy engine of the factory.
     */
    public final SolvableSyzygyAbstract<C> engine;


    /**
     * Groebner base engine.
     */
    protected final SolvableGroebnerBaseAbstract<C> bb;


    /**
     * Indicator if this ring is a field.
     */
    protected int isField = -1; // initially unknown


    /**
     * The constructor creates a SolvableLocalRing object from a SolvableIdeal.
     * @param i solvable localization polynomial ideal.
     */
    public SolvableLocalRing(SolvableIdeal<C> i) {
        if (i == null) {
            throw new IllegalArgumentException("ideal may not be null");
        }
        ring = i.getRing();
        ideal = i.GB(); // cheap if isGB
        if (ideal.isONE()) {
            throw new IllegalArgumentException("ideal may not be 1");
        }
        if (ideal.isMaximal()) {
            isField = 1;
        } else {
            isField = 0;
            logger.warn("ideal not maximal");
            //throw new IllegalArgumentException("ideal must be maximal");
        }
        engine = new SolvableSyzygyAbstract<C>();
        bb = new SolvableGroebnerBaseSeq<C>();
        logger.debug("solvable local ring constructed");
    }


    /**
     * Least common multiple.
     * @param n first solvable polynomial.
     * @param d second solvable polynomial.
     * @return lcm(n,d)
     */
    protected GenSolvablePolynomial<C> syzLcm(GenSolvablePolynomial<C> n, GenSolvablePolynomial<C> d) {
        List<GenSolvablePolynomial<C>> list = new ArrayList<GenSolvablePolynomial<C>>(1);
        list.add(n);
        SolvableIdeal<C> N = new SolvableIdeal<C>(n.ring, list, true);
        list = new ArrayList<GenSolvablePolynomial<C>>(1);
        list.add(d);
        SolvableIdeal<C> D = new SolvableIdeal<C>(n.ring, list, true);
        SolvableIdeal<C> L = N.intersect(D);
        if (L.getList().size() != 1) {
            throw new RuntimeException("lcm not uniqe");
        }
        GenSolvablePolynomial<C> lcm = L.getList().get(0);
        return lcm;
    }


    /**
     * Greatest common divisor.
     * @param n first solvable polynomial.
     * @param d second solvable polynomial.
     * @return gcd(n,d)
     */
    protected GenSolvablePolynomial<C> syzGcd(GenSolvablePolynomial<C> n, GenSolvablePolynomial<C> d) {
        if (n.isZERO()) {
            return d;
        }
        if (d.isZERO()) {
            return n;
        }
        if (n.isONE()) {
            return n;
        }
        if (d.isONE()) {
            return d;
        }
        GenSolvablePolynomial<C> p = n.multiply(d);
        GenSolvablePolynomial<C> lcm = syzLcm(n, d);
        // divide
        List<GenSolvablePolynomial<C>> Q = new ArrayList<GenSolvablePolynomial<C>>(1);
        Q.add(ring.getZERO());
        List<GenSolvablePolynomial<C>> V = new ArrayList<GenSolvablePolynomial<C>>(1);
        V.add(lcm);
        // GenSolvablePolynomial<C> gcd = divide(p, lcm);
        GenSolvablePolynomial<C> x = engine.sred.leftNormalform(Q, V, p);
        GenSolvablePolynomial<C> y = Q.get(0);
        return y;
    }


    /**
     * Is this structure finite or infinite.
     * @return true if this structure is finite, else false.
     * @see edu.jas.structure.ElemFactory#isFinite()
     */
    public boolean isFinite() {
        return ring.isFinite() && bb.commonZeroTest(ideal.getList()) <= 0;
    }


    /**
     * Copy SolvableLocal element c.
     * @param c
     * @return a copy of c.
     */
    public SolvableLocal<C> copy(SolvableLocal<C> c) {
        return new SolvableLocal<C>(c.ring, c.num, c.den, true);
    }


    /**
     * Get the zero element.
     * @return 0 as SolvableLocal.
     */
    public SolvableLocal<C> getZERO() {
        return new SolvableLocal<C>(this, ring.getZERO());
    }


    /**
     * Get the one element.
     * @return 1 as SolvableLocal.
     */
    public SolvableLocal<C> getONE() {
        return new SolvableLocal<C>(this, ring.getONE());
    }


    /**
     * Get a list of the generating elements.
     * @return list of generators for the algebraic structure.
     * @see edu.jas.structure.ElemFactory#generators()
     */
    public List<SolvableLocal<C>> generators() {
        List<GenSolvablePolynomial<C>> pgens = PolynomialList.<C> castToSolvableList(ring.generators());
        List<SolvableLocal<C>> gens = new ArrayList<SolvableLocal<C>>(pgens.size() * 2 - 1);
        GenSolvablePolynomial<C> one = ring.getONE();
        for (GenSolvablePolynomial<C> p : pgens) {
            SolvableLocal<C> q = new SolvableLocal<C>(this, p);
            gens.add(q);
            if (!p.isONE() && !ideal.contains(p)) { // q.isUnit()
                q = new SolvableLocal<C>(this, one, p);
                gens.add(q);
            }
        }
        return gens;
    }


    /**
     * Query if this ring is commutative.
     * @return true if this ring is commutative, else false.
     */
    public boolean isCommutative() {
        return ring.isCommutative();
    }


    /**
     * Query if this ring is associative.
     * @return true if this ring is associative, else false.
     */
    public boolean isAssociative() {
        if (!ring.isAssociative()) {
            return false;
        }
        SolvableLocal<C> Xi, Xj, Xk, p, q;
        List<SolvableLocal<C>> gens = generators();
        int ngen = gens.size();
        for (int i = 0; i < ngen; i++) {
            Xi = gens.get(i);
            for (int j = i + 1; j < ngen; j++) {
                Xj = gens.get(j);
                for (int k = j + 1; k < ngen; k++) {
                    Xk = gens.get(k);
                    try {
                        p = Xk.multiply(Xj).multiply(Xi);
                        q = Xk.multiply(Xj.multiply(Xi));
                    } catch (IllegalArgumentException e) {
                        //e.printStackTrace();
                        continue; // ignore undefined multiplication
                    }
                    if (!p.equals(q)) {
                        if (true || debug) {
                            logger.info("Xk = " + Xk + ", Xj = " + Xj + ", Xi = " + Xi);
                            logger.info("p = ( Xk * Xj ) * Xi = " + p);
                            logger.info("q = Xk * ( Xj * Xi ) = " + q);
                        }
                        return false;
                    }
                }
            }
        }
        return true;
    }


    /**
     * Query if this ring is a field.
     * @return false.
     */
    public boolean isField() {
        if (isField > 0) {
            return true;
        }
        if (isField == 0) {
            return false;
        }
        // not reached
        return false;
    }


    /**
     * Characteristic of this ring.
     * @return characteristic of this ring.
     */
    public java.math.BigInteger characteristic() {
        return ring.characteristic();
    }


    /**
     * Get a SolvableLocal element from a BigInteger value.
     * @param a BigInteger.
     * @return a SolvableLocal.
     */
    public SolvableLocal<C> fromInteger(java.math.BigInteger a) {
        return new SolvableLocal<C>(this, ring.fromInteger(a));
    }


    /**
     * Get a SolvableLocal element from a long value.
     * @param a long.
     * @return a SolvableLocal.
     */
    public SolvableLocal<C> fromInteger(long a) {
        return new SolvableLocal<C>(this, ring.fromInteger(a));
    }


    /**
     * Get the String representation as RingFactory.
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "SolvableLocalRing[ " + ideal.toString() + " ]";
    }


    /**
     * Get a scripting compatible string representation.
     * @return script compatible representation for this ElemFactory.
     * @see edu.jas.structure.ElemFactory#toScript()
     */
    @Override
    public String toScript() {
        // Python case
        return "SLC(" + ideal.list.toScript() + ")";
    }


    /**
     * Comparison with any other object.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object b) {
        if (!(b instanceof SolvableLocalRing)) {
            return false;
        }
        SolvableLocalRing<C> a = null;
        try {
            a = (SolvableLocalRing<C>) b;
        } catch (ClassCastException e) {
        }
        if (a == null) {
            return false;
        }
        if (!ring.equals(a.ring)) {
            return false;
        }
        return ideal.equals(a.ideal);
    }


    /**
     * Hash code for this local ring.
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int h;
        h = ideal.hashCode();
        return h;
    }


    /**
     * SolvableLocal random.
     * @param n such that 0 &le; v &le; (2<sup>n</sup>-1).
     * @return a random residue element.
     */
    public SolvableLocal<C> random(int n) {
        GenSolvablePolynomial<C> r = ring.random(n).monic();
        r = ideal.normalform(r);
        GenSolvablePolynomial<C> s;
        do {
            s = ring.random(n).monic();
            s = ideal.normalform(s);
        } while (s.isZERO());
        return new SolvableLocal<C>(this, r, s, false);
    }


    /**
     * Generate a random residum polynomial.
     * @param k bitsize of random coefficients.
     * @param l number of terms.
     * @param d maximal degree in each variable.
     * @param q density of nozero exponents.
     * @return a random residue polynomial.
     */
    public SolvableLocal<C> random(int k, int l, int d, float q) {
        GenSolvablePolynomial<C> r = ring.random(k, l, d, q).monic();
        r = ideal.normalform(r);
        GenSolvablePolynomial<C> s;
        do {
            s = ring.random(k, l, d, q).monic();
            s = ideal.normalform(s);
        } while (s.isZERO());
        return new SolvableLocal<C>(this, r, s, false);
    }


    /**
     * SolvableLocal random.
     * @param n such that 0 &le; v &le; (2<sup>n</sup>-1).
     * @param rnd is a source for random bits.
     * @return a random residue element.
     */
    public SolvableLocal<C> random(int n, Random rnd) {
        GenSolvablePolynomial<C> r = ring.random(n, rnd).monic();
        r = ideal.normalform(r);
        GenSolvablePolynomial<C> s;
        do {
            s = ring.random(n).monic();
            s = ideal.normalform(s);
        } while (s.isZERO());
        return new SolvableLocal<C>(this, r, s, false);
    }


    /**
     * Parse SolvableLocal from String.
     * @param s String.
     * @return SolvableLocal from s.
     */
    public SolvableLocal<C> parse(String s) {
        int i = s.indexOf("{");
        if (i >= 0) {
            s = s.substring(i + 1);
        }
        i = s.lastIndexOf("}");
        if (i >= 0) {
            s = s.substring(0, i);
        }
        i = s.indexOf("|");
        if (i < 0) {
            GenSolvablePolynomial<C> n = ring.parse(s);
            return new SolvableLocal<C>(this, n);
        }
        String s1 = s.substring(0, i);
        String s2 = s.substring(i + 1);
        GenSolvablePolynomial<C> n = ring.parse(s1);
        GenSolvablePolynomial<C> d = ring.parse(s2);
        return new SolvableLocal<C>(this, n, d);
    }


    /**
     * Parse SolvableLocal from Reader.
     * @param r Reader.
     * @return next SolvableLocal from r.
     */
    public SolvableLocal<C> parse(Reader r) {
        String s = StringUtil.nextPairedString(r, '{', '}');
        return parse(s);
    }

}