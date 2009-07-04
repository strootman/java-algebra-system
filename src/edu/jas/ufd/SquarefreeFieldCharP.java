/*
 * $Id$
 */

package edu.jas.ufd;


import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import edu.jas.structure.GcdRingElem;
import edu.jas.structure.RingFactory;
import edu.jas.structure.UnaryFunctor;
import edu.jas.structure.Power;

import edu.jas.poly.ExpVector;
import edu.jas.poly.Monomial;
import edu.jas.poly.GenPolynomial;
import edu.jas.poly.GenPolynomialRing;
import edu.jas.poly.PolyUtil;

import edu.jas.util.ListUtil;


/**
 * Squarefree decomposition for coefficient fields of characteristic p.
 * @author Heinz Kredel
 */

public abstract class SquarefreeFieldCharP<C extends GcdRingElem<C>> 
                      extends SquarefreeAbstract<C> /*implements Squarefree<C>*/ {
 

    private static final Logger logger = Logger.getLogger(SquarefreeFieldCharP.class);


    private final boolean debug = logger.isDebugEnabled();


    /**
     * Factory for finite field of characteristic p coefficients.
     */
    protected final RingFactory<C> coFac;


    /**
     * GCD engine for finite field of characteristic p base coefficients.
     */
    protected final GreatestCommonDivisorAbstract<C> engine;



    /**
     * Constructor. 
     */
    public SquarefreeFieldCharP(RingFactory<C> fac) {
//         isFinite() predicate not yet present
//         if ( !fac.isFinite() ) {
//             throw new IllegalArgumentException("fac must be finite"); 
//         }
        if ( !fac.isField() ) {
            throw new IllegalArgumentException("fac must be a field"); 
        }
        if ( fac.characteristic().signum() == 0 ) {
            throw new IllegalArgumentException("characterisic(fac) must be non-zero"); 
        }
        coFac = fac;
        //engine = GCDFactory.<C>getImplementation( fac );
        engine = GCDFactory.<C>getProxy( fac );
    }


    /** Get the String representation.
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getClass().getName() + " with " + engine + " over " + coFac;
    }


    /**
     * GenPolynomial polynomial greatest squarefree divisor.
     * @param P GenPolynomial.
     * @return squarefree(pp(P)).
     */
    public GenPolynomial<C> baseSquarefreePart(GenPolynomial<C> P) {
        if (P == null || P.isZERO()) {
            return P;
        }
        GenPolynomialRing<C> pfac = P.ring;
        if (pfac.nvar > 1) {
            throw new RuntimeException(this.getClass().getName()
                    + " only for univariate polynomials");
        }
        // just for the moment:
        GenPolynomial<C> s = pfac.getONE();
        SortedMap<GenPolynomial<C>,Long> factors = baseSquarefreeFactors(P);
        System.out.println("sqfPart,factors = " + factors);
        for ( GenPolynomial<C> sp : factors.keySet() ) {
            s = s.multiply(sp);
        }
        return s.monic();
    }


    /**
     * GenPolynomial polynomial squarefree factorization.
     * @param A GenPolynomial.
     * @return [p_1 -> e_1, ..., p_k -> e_k] with P = prod_{i=1,...,k} p_i^{e_i} and p_i squarefree.
     */
    public SortedMap<GenPolynomial<C>,Long> baseSquarefreeFactors( GenPolynomial<C> A ) {
        SortedMap<GenPolynomial<C>,Long> sfactors = new TreeMap<GenPolynomial<C>,Long>();
        if ( A == null || A.isZERO() ) {
            return sfactors;
        }
        if ( A.isConstant() ) {
            sfactors.put(A,1L);
            return sfactors;
        }
        GenPolynomialRing<C> pfac = A.ring;
        if ( pfac.nvar > 1 ) {
            throw new RuntimeException(this.getClass().getName()
                    + " only for univariate polynomials");
        }
        C ldbcf = A.leadingBaseCoefficient();
        if ( ! ldbcf.isONE() ) {
            A = A.divide(ldbcf);
            GenPolynomial<C> f1 = pfac.getONE().multiply(ldbcf);
            //System.out.println("gcda sqf f1 = " + f1);
            sfactors.put(f1,1L);
            ldbcf = pfac.coFac.getONE();
        }
        GenPolynomial<C> T0 = A;
        long e = 1L;
        GenPolynomial<C> Tp;
        GenPolynomial<C> T = null;
        GenPolynomial<C> V = null;
        long k = 0L;
        long mp = 0L;
        boolean init = true;
        while ( true ) { 
            if ( init ) {
                if ( T0.isConstant() || T0.isZERO() ) {
                     break;
                }
                Tp = PolyUtil.<C> baseDeriviative(T0);
                T = engine.baseGcd(T0,Tp);
                T = T.monic();
                V = PolyUtil.<C> basePseudoDivide(T0,T);
                System.out.println("iT0 = " + T0);
                System.out.println("iTp = " + Tp);
                System.out.println("iT  = " + T);
                System.out.println("iV  = " + V);
                k = 0L;
                mp = 0L;
                init = false;
            }
            if ( V.isConstant() ) { 
                mp = pfac.characteristic().longValue(); // assert != 0
                   //T0 = PolyUtil.<C> baseModRoot(T,mp);
                T0 = baseRootCharacteristic(T);
                if ( T0 == null ) {
                    break;
                    //throw new RuntimeException("can not happen");
                }
                System.out.println("char root: T0 = " + T0 + ", T = " + T);
                e = e * mp;
                init = true;
                continue;
            }
            k++;
            if ( mp != 0L && k % mp == 0L ) {
                T = PolyUtil.<C> basePseudoDivide(T, V);
                System.out.println("k = " + k);
                //System.out.println("T = " + T);
                k++;
            }
            GenPolynomial<C> W = engine.baseGcd(T,V);
            W = W.monic();
            GenPolynomial<C> z = PolyUtil.<C> basePseudoDivide(V, W);
            //System.out.println("W = " + W);
            //System.out.println("z = " + z);
            V = W;
            T = PolyUtil.<C> basePseudoDivide(T, V);
            //System.out.println("V = " + V);
            //System.out.println("T = " + T);
            if ( z.degree(0) > 0 ) {
                if ( ldbcf.isONE() && !z.leadingBaseCoefficient().isONE() ) {
                    z = z.monic();
                    System.out.println("z,monic = " + z);
                }
                sfactors.put(z, (e*k));
            }
        }
//      look, a stupid error:
//         if ( !ldbcf.isONE() ) {
//             GenPolynomial<C> f1 = sfactors.firstKey();
//             long e1 = sfactors.remove(f1);
//             System.out.println("gcda sqf c = " + c);
//             f1 = f1.multiply(c);
//             //System.out.println("gcda sqf f1e = " + f1);
//             sfactors.put(f1,e1);
//         }
        return sfactors;
    }


    /**
     * GenPolynomial recursive univariate polynomial greatest squarefree divisor.
     * @param P recursive univariate GenPolynomial.
     * @return squarefree(pp(P)).
     */
    public GenPolynomial<GenPolynomial<C>> 
      recursiveUnivariateSquarefreePart( GenPolynomial<GenPolynomial<C>> P ) {
        if ( P == null || P.isZERO() ) {
            return P;
        }
        GenPolynomialRing<GenPolynomial<C>> pfac = P.ring;
        if ( pfac.nvar > 1 ) {
            throw new RuntimeException(this.getClass().getName()
                    + " only for multivariate polynomials");
        }
        // just for the moment:
        GenPolynomial<GenPolynomial<C>> s = pfac.getONE();

        SortedMap<GenPolynomial<GenPolynomial<C>>,Long> factors = recursiveUnivariateSquarefreeFactors(P);
        System.out.println("sqfPart,factors = " + factors);
        for ( GenPolynomial<GenPolynomial<C>> sp : factors.keySet() ) {
            s = s.multiply(sp);
        }
        return PolyUtil.<C>monic(s);
    }


   /**
     * GenPolynomial recursive univariate polynomial squarefree factorization.
     * @param P recursive univariate GenPolynomial.
     * @return [p_1 -> e_1, ..., p_k -> e_k] with P = prod_{i=1,...,k} p_i^{e_i} and p_i squarefree.
     */
    public SortedMap<GenPolynomial<GenPolynomial<C>>,Long> 
      recursiveUnivariateSquarefreeFactors( GenPolynomial<GenPolynomial<C>> P ) {
        SortedMap<GenPolynomial<GenPolynomial<C>>,Long> sfactors 
                 = new TreeMap<GenPolynomial<GenPolynomial<C>>,Long>();
        if (P == null || P.isZERO()) {
            return sfactors;
        }
        GenPolynomialRing<GenPolynomial<C>> pfac = P.ring;
        if (pfac.nvar > 1) {
            // recursiveContent not possible by return type
            throw new RuntimeException(this.getClass().getName()
                    + " only for univariate polynomials");
        }
        // if base coefficient ring is a field, make monic
        GenPolynomialRing<C> cfac = (GenPolynomialRing<C>)pfac.coFac;
        C ldbcf = P.leadingBaseCoefficient().leadingBaseCoefficient();
        if ( ! ldbcf.isONE() ) {
            GenPolynomial<C> lc = cfac.getONE().multiply(ldbcf);
            GenPolynomial<GenPolynomial<C>> pl = pfac.getONE().multiply(lc);
            sfactors.put(pl,1L);
            C li = ldbcf.inverse();
            System.out.println("li = " + li);
            P = P.multiply( cfac.getONE().multiply(li) );
            //System.out.println("P,monic = " + P);
            ldbcf = P.leadingBaseCoefficient().leadingBaseCoefficient();
        }
        // factors of content
        GenPolynomial<C> Pc = engine.recursiveContent(P);
        System.out.println("Pc = " + Pc);
        C cldbcf = Pc.leadingBaseCoefficient();
        if ( ! cldbcf.isONE() ) {
            C li = cldbcf.inverse();
            System.out.println("cli = " + li);
            Pc = Pc.multiply(li);
            //System.out.println("Pc,monic = " + Pc);
        }
        if ( !Pc.isONE() ) {
            P = PolyUtil.<C> coefficientPseudoDivide(P,Pc);
        }
        SortedMap<GenPolynomial<C>,Long> rsf = squarefreeFactors(Pc);
        System.out.println("rsf = " + rsf);
        // add factors of content
        for (GenPolynomial<C> c : rsf.keySet()) {
            if ( !c.isONE() ) {
               GenPolynomial<GenPolynomial<C>>  cr = pfac.getONE().multiply(c);
               Long rk = rsf.get(c);
               sfactors.put(cr,rk);
            }
        }

        // factors of recursive polynomial
        GenPolynomial<GenPolynomial<C>> T0 = P;
        long e = 1L;
        GenPolynomial<GenPolynomial<C>> Tp;
        GenPolynomial<GenPolynomial<C>> T = null;
        GenPolynomial<GenPolynomial<C>> V = null;
        long k = 0L;
        long mp = 0L;
        boolean init = true;
        while ( true ) { 
            if ( init ) {
                if ( T0.isConstant() || T0.isZERO() ) {
                     break;
                }
                Tp = PolyUtil.<C> recursiveDeriviative(T0);
                T = engine.recursiveUnivariateGcd(T0,Tp);
                C tl = T.leadingBaseCoefficient().leadingBaseCoefficient();
                if ( ! tl.isONE() ) {
                    C ti = tl.inverse();
                    System.out.println("ti = " + ti);
                    GenPolynomial<C> tc = cfac.getONE().multiply(ti);
                    T = T.multiply(tc);
                }
                V = PolyUtil.<C> recursivePseudoDivide(T0,T);
                //System.out.println("iT0 = " + T0);
                //System.out.println("iTp = " + Tp);
                //System.out.println("iT = " + T);
                //System.out.println("iV = " + V);
                k = 0L;
                mp = 0L;
                init = false;
            }
            if ( V.isConstant() ) { 
                mp = pfac.characteristic().longValue(); // assert != 0
                    //T0 = PolyUtil.<C> recursiveModRoot(T,mp);
                T0 = recursiveUnivariateRootCharacteristic(T);
                System.out.println("char root: T0r = " + T0 + ", Tr = " + T);
                if ( T0 == null ) {
                    break;
                }
                e = e * mp;
                init = true;
                continue;
            }
            k++;
            if ( mp != 0L && k % mp == 0L ) {
                T = PolyUtil.<C> recursivePseudoDivide(T, V);
                System.out.println("k = " + k);
                //System.out.println("T = " + T);
                k++;
            }
            GenPolynomial<GenPolynomial<C>> W = engine.recursiveUnivariateGcd(T,V);
            C wl = W.leadingBaseCoefficient().leadingBaseCoefficient();
            if ( ! wl.isONE() ) {
                C wi = wl.inverse();
                System.out.println("wi = " + wi);
                GenPolynomial<C> wc = cfac.getONE().multiply(wi);
                W = W.multiply(wc);
            }
            GenPolynomial<GenPolynomial<C>> z = PolyUtil.<C> recursivePseudoDivide(V, W);
            //System.out.println("W = " + W);
            //System.out.println("z = " + z);
            V = W;
            T = PolyUtil.<C> recursivePseudoDivide(T, V);
            //System.out.println("V = " + V);
            //System.out.println("T = " + T);
            //was: if ( z.degree(0) > 0 ) {
            if ( !z.isONE() && !z.isZERO() ) {
                C zl = z.leadingBaseCoefficient().leadingBaseCoefficient();
                if ( ldbcf.isONE() && !zl.isONE() ) {
                    C li = zl.inverse();
                    GenPolynomial<C> lc = cfac.getONE().multiply(li);
                    //System.out.println("lc = " + lc);
                    z = z.multiply(lc);
                    //System.out.println("z,monic = " + z);
                }
                System.out.println("z,put = " + z);
                sfactors.put(z, (e*k));
            }
        }
        return sfactors;
    }


    /**
     * GenPolynomial greatest squarefree divisor.
     * @param P GenPolynomial.
     * @return squarefree(pp(P)).
     */
    public GenPolynomial<C> squarefreePart(GenPolynomial<C> P) {
        if (P == null) {
            throw new RuntimeException(this.getClass().getName() + " P != null");
        }
        if (P.isZERO()) {
            return P;
        }
        GenPolynomialRing<C> pfac = P.ring;
        if (pfac.nvar <= 1) {
            return baseSquarefreePart(P);
        }
        // just for the moment:
        GenPolynomial<C> s = pfac.getONE();
        SortedMap<GenPolynomial<C>,Long> factors = squarefreeFactors(P);
        System.out.println("sqfPart,factors = " + factors);
        for ( GenPolynomial<C> sp : factors.keySet() ) {
            s = s.multiply(sp);
        }
        return s.monic();
    }


    /**
     * GenPolynomial squarefree factorization.
     * @param P GenPolynomial.
     * @return [p_1 -> e_1, ..., p_k -> e_k] with P = prod_{i=1,...,k} p_i^{e_i} and p_i squarefree.
     */
    public SortedMap<GenPolynomial<C>,Long> squarefreeFactors(GenPolynomial<C> P) {
        if (P == null) {
            throw new RuntimeException(this.getClass().getName() + " P != null");
        }
        GenPolynomialRing<C> pfac = P.ring;
        if (pfac.nvar <= 1) {
            return baseSquarefreeFactors(P);
        }
        SortedMap<GenPolynomial<C>,Long> sfactors = new TreeMap<GenPolynomial<C>,Long>();
        if (P.isZERO()) {
            return sfactors;
        }
        GenPolynomialRing<C> cfac = pfac.contract(1);
        GenPolynomialRing<GenPolynomial<C>> rfac 
            = new GenPolynomialRing<GenPolynomial<C>>(cfac, 1);

        GenPolynomial<GenPolynomial<C>> Pr = PolyUtil.<C> recursive(rfac, P);
        SortedMap<GenPolynomial<GenPolynomial<C>>,Long> PP = recursiveUnivariateSquarefreeFactors(Pr);

        for (Map.Entry<GenPolynomial<GenPolynomial<C>>,Long> m : PP.entrySet()) {
            Long i = m.getValue();
            GenPolynomial<GenPolynomial<C>> Dr = m.getKey();
            GenPolynomial<C> D = PolyUtil.<C> distribute(pfac, Dr);
            sfactors.put(D,i);
        }
        return sfactors;
    }


    /**
     * GenPolynomial squarefree and co-prime list.
     * @param A list of GenPolynomials.
     * @return B with gcd(b,c) = 1 for all b != c in B 
     *         and for all non-constant a in A there exists b in B with b|a
     *         and each b in B is squarefree. 
     *         B does not contain zero or constant polynomials.
     */
    public List<GenPolynomial<C>> coPrimeSquarefree(List<GenPolynomial<C>> A) {
        if (A == null || A.isEmpty()) {
            return A;
        }
        List<GenPolynomial<C>> S = new ArrayList<GenPolynomial<C>>();
        for ( GenPolynomial<C> g : A ) {
            SortedMap<GenPolynomial<C>,Long> sm = squarefreeFactors(g);
            S.addAll( sm.keySet() );
        }
        List<GenPolynomial<C>> B = engine.coPrime(S);
        return B;
    }



    /* --------- char-th roots --------------------- */


    /**
     * GenPolynomial char-th root main variable.
     * Base coefficient type must be finite field, 
     * that is ModInteger or AlgebraicNumber&lt;ModInteger&gt; etc.
     * @param P GenPolynomial.
     * @return char-th_rootOf(P), or null if no char-th root.
     */
    public abstract GenPolynomial<C> baseRootCharacteristic( GenPolynomial<C> P );


    /**
     * GenPolynomial char-th root main variable.
     * @param P recursive univariate GenPolynomial.
     * @return char-th_rootOf(P), or null if P is no char-th root.
     */
    // param <C> base coefficient type must be ModInteger.
    public abstract GenPolynomial<GenPolynomial<C>> 
        recursiveUnivariateRootCharacteristic( GenPolynomial<GenPolynomial<C>> P );



    /**
     * Polynomial is char-th root.
     * @param P polynomial.
     * @param F = [p_1 -&gt; e_1, ..., p_k -&gt; e_k].
     * @return true if P = prod_{i=1,...,k} p_i**(e_i*p), else false.
     */
    public boolean isCharRoot(GenPolynomial<C> P, SortedMap<GenPolynomial<C>, Long> F) {
        if (P == null || F == null) {
            throw new IllegalArgumentException("P and F may not be null");
        }
        if (P.isZERO() && F.size() == 0) {
            return true;
        }
        GenPolynomial<C> t = P.ring.getONE();
        long p = P.ring.characteristic().longValue();
        for (GenPolynomial<C> f : F.keySet()) {
            Long E = F.get(f);
            long e = E.longValue();
            GenPolynomial<C> g = Power.<GenPolynomial<C>> positivePower(f, e);
            if ( !f.isConstant() ) { 
               g = Power.<GenPolynomial<C>> positivePower(g, p);
            }
            t = t.multiply(g);
        }
        boolean f = P.equals(t) || P.equals(t.negate());
        if (!f) {
            System.out.println("\nfactorization(map): " + f);
            System.out.println("P = " + P);
            System.out.println("t = " + t);
            P = P.monic();
            t = t.monic();
            f = P.equals(t) || P.equals(t.negate());
            if ( f ) {
                return f;
            }
            System.out.println("\nfactorization(map): " + f);
            System.out.println("P = " + P);
            System.out.println("t = " + t);
        }
        return f;
    }


    /**
     * Recursive polynomial is char-th root.
     * @param P recursive polynomial.
     * @param F = [p_1 -&gt; e_1, ..., p_k -&gt; e_k].
     * @return true if P = prod_{i=1,...,k} p_i**(e_i*p), else false.
     */
    public boolean isRecursiveCharRoot(GenPolynomial<GenPolynomial<C>> P, SortedMap<GenPolynomial<GenPolynomial<C>>, Long> F) {
        if (P == null || F == null) {
            throw new IllegalArgumentException("P and F may not be null");
        }
        if (P.isZERO() && F.size() == 0) {
            return true;
        }
        GenPolynomial<GenPolynomial<C>> t = P.ring.getONE();
        long p = P.ring.characteristic().longValue();
        for (GenPolynomial<GenPolynomial<C>> f : F.keySet()) {
            Long E = F.get(f);
            long e = E.longValue();
            GenPolynomial<GenPolynomial<C>> g = Power.<GenPolynomial<GenPolynomial<C>>> positivePower(f, e);
            if ( !f.isConstant() ) { 
               g = Power.<GenPolynomial<GenPolynomial<C>>> positivePower(g, p);
            }
            t = t.multiply(g);
        }
        boolean f = P.equals(t) || P.equals(t.negate());
        if (!f) {
            System.out.println("\nfactorization(map): " + f);
            System.out.println("P = " + P);
            System.out.println("t = " + t);
            P = P.monic();
            t = t.monic();
            f = P.equals(t) || P.equals(t.negate());
            if ( f ) {
                return f;
            }
            System.out.println("\nfactorization(map): " + f);
            System.out.println("P = " + P);
            System.out.println("t = " + t);
        }
        return f;
    }


    /**
     * Recursive polynomial is char-th root.
     * @param P recursive polynomial.
     * @param r = recursive polynomial.
     * @return true if P = r**p, else false.
     */
    public boolean isRecursiveCharRoot(GenPolynomial<GenPolynomial<C>> P, GenPolynomial<GenPolynomial<C>> r) {
        if (P == null || r == null) {
            throw new IllegalArgumentException("P and r may not be null");
        }
        if (P.isZERO() && r.isZERO()) {
            return true;
        }
        long p = P.ring.characteristic().longValue();
        GenPolynomial<GenPolynomial<C>> t = Power.<GenPolynomial<GenPolynomial<C>>> positivePower(r, p);

        boolean f = P.equals(t) || P.equals(t.negate());
        if (!f) {
            System.out.println("\nisCharRoot: " + f);
            System.out.println("P = " + P);
            System.out.println("t = " + t);
            P = P.monic();
            t = t.monic();
            f = P.equals(t) || P.equals(t.negate());
            if ( f ) {
                return f;
            }
            System.out.println("\nisCharRoot: " + f);
            System.out.println("P = " + P);
            System.out.println("t = " + t);
        }
        return f;
    }

}







