/*
 * $Id$
 */

package edu.jas.ufd;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.BitSet;

import org.apache.log4j.Logger;

import edu.jas.application.Quotient;
import edu.jas.application.QuotientRing;

import edu.jas.arith.BigInteger;
import edu.jas.arith.Modular;
import edu.jas.arith.ModInteger;
import edu.jas.arith.ModIntegerRing;
import edu.jas.arith.ModLong;
import edu.jas.arith.ModLongRing;

import edu.jas.poly.AlgebraicNumber;
import edu.jas.poly.AlgebraicNumberRing;
import edu.jas.poly.ExpVector;
import edu.jas.poly.GenPolynomial;
import edu.jas.poly.GenPolynomialRing;
import edu.jas.poly.PolyUtil;

import edu.jas.structure.GcdRingElem;
import edu.jas.structure.RingElem;
import edu.jas.structure.RingFactory;
import edu.jas.structure.ModularRingFactory;
import edu.jas.structure.UnaryFunctor;

import edu.jas.util.ListUtil;


/**
 * Hensel utilities for ufd.
 * @author Heinz Kredel
 */

public class HenselUtil {


    private static final Logger logger = Logger.getLogger(HenselUtil.class);


    private static boolean debug = logger.isDebugEnabled();


    /**
     * Modular Hensel lifting algorithm on coefficients. Let p =
     * A.ring.coFac.modul() = B.ring.coFac.modul() and assume C == A*B mod p
     * with ggt(A,B) == 1 mod p and S A + T B == 1 mod p. See Algorithm 6.1. in
     * Geddes et.al.. Linear version, as it does not lift S A + T B == 1 mod
     * p^{e+1}.
     * @param C GenPolynomial
     * @param A GenPolynomial
     * @param B other GenPolynomial
     * @param S GenPolynomial
     * @param T GenPolynomial
     * @param M bound on the coefficients of A1 and B1 as factors of C.
     * @return [A1,B1,Am,Bm] = lift(C,A,B), with C = A1 * B1 mod p^e, Am = A1 mod p^e, Bm = B1 mod p^e .
     */
    @SuppressWarnings("unchecked")
    public static <MOD extends GcdRingElem<MOD> & Modular>
      HenselApprox<MOD> liftHensel(GenPolynomial<BigInteger> C, BigInteger M, 
                                   GenPolynomial<MOD> A, GenPolynomial<MOD> B, 
                                   GenPolynomial<MOD> S, GenPolynomial<MOD> T) throws NoLiftingException {
        if (C == null || C.isZERO()) {
            return new HenselApprox<MOD>(C,C,A,B);
        }
        if (A == null || A.isZERO() || B == null || B.isZERO()) {
            throw new RuntimeException("A and B must be nonzero");
        }
        GenPolynomialRing<BigInteger> fac = C.ring;
        if (fac.nvar != 1) { // todo assert
            throw new RuntimeException("polynomial ring not univariate");
        }
        // setup factories
        GenPolynomialRing<MOD> pfac = A.ring;
        RingFactory<MOD> p = pfac.coFac;
        RingFactory<MOD> q = p;
        ModularRingFactory<MOD> P = (ModularRingFactory<MOD>) p;
        ModularRingFactory<MOD> Q = (ModularRingFactory<MOD>) q;
        BigInteger Qi = Q.getIntegerModul();
        BigInteger M2 = M.multiply(M.fromInteger(2));
        BigInteger Mq = Qi;

        // normalize c and a, b factors, assert p is prime
        GenPolynomial<BigInteger> Ai;
        GenPolynomial<BigInteger> Bi;
        BigInteger c = C.leadingBaseCoefficient();
        C = C.multiply(c); // sic
        MOD a = A.leadingBaseCoefficient();
        if (!a.isONE()) { // A = A.monic();
            A = A.divide(a);
            S = S.multiply(a);
        }
        MOD b = B.leadingBaseCoefficient();
        if (!b.isONE()) { // B = B.monic();
            B = B.divide(b);
            T = T.multiply(b);
        }
        MOD ci = P.fromInteger(c.getVal());
        A = A.multiply(ci);
        B = B.multiply(ci);
        T = T.divide(ci);
        S = S.divide(ci);
        Ai = PolyUtil.integerFromModularCoefficients(fac, A);
        Bi = PolyUtil.integerFromModularCoefficients(fac, B);
        // replace leading base coefficients
        ExpVector ea = Ai.leadingExpVector();
        ExpVector eb = Bi.leadingExpVector();
        Ai.doPutToMap(ea, c);
        Bi.doPutToMap(eb, c);

        // polynomials mod p
        GenPolynomial<MOD> Ap;
        GenPolynomial<MOD> Bp;
        GenPolynomial<MOD> A1p = A;
        GenPolynomial<MOD> B1p = B;
        GenPolynomial<MOD> Ep;

        // polynomials over the integers
        GenPolynomial<BigInteger> E;
        GenPolynomial<BigInteger> Ea;
        GenPolynomial<BigInteger> Eb;
        GenPolynomial<BigInteger> Ea1;
        GenPolynomial<BigInteger> Eb1;

        while (Mq.compareTo(M2) < 0) {
            // compute E=(C-AB)/q over the integers
            E = C.subtract(Ai.multiply(Bi));
            if (E.isZERO()) {
                //System.out.println("leaving on zero error");
                logger.info("leaving on zero error");
                break;
            }
            try {
                E = E.divide(Qi);
            } catch (RuntimeException e) {
                // useful in debuging
                //System.out.println("C  = " + C );
                //System.out.println("Ai = " + Ai );
                //System.out.println("Bi = " + Bi );
                //System.out.println("E  = " + E );
                //System.out.println("Qi = " + Qi );
                throw e;
            }
            // E mod p
            Ep = PolyUtil.<MOD> fromIntegerCoefficients(pfac, E);
            //logger.info("Ep = " + Ep);

            // construct approximation mod p
            Ap = S.multiply(Ep); // S,T ++ T,S
            Bp = T.multiply(Ep);
            GenPolynomial<MOD>[] QR;
            QR = Ap.divideAndRemainder(B);
            GenPolynomial<MOD> Qp;
            GenPolynomial<MOD> Rp;
            Qp = QR[0];
            Rp = QR[1];
            A1p = Rp;
            B1p = Bp.sum(A.multiply(Qp));

            // construct q-adic approximation, convert to integer
            Ea = PolyUtil.integerFromModularCoefficients(fac, A1p);
            Eb = PolyUtil.integerFromModularCoefficients(fac, B1p);
            Ea1 = Ea.multiply(Qi);
            Eb1 = Eb.multiply(Qi);

            Ea = Ai.sum(Eb1); // Eb1 and Ea1 are required
            Eb = Bi.sum(Ea1); //--------------------------
            assert (Ea.degree(0) + Eb.degree(0) <= C.degree(0));
            //if ( Ea.degree(0)+Eb.degree(0) > C.degree(0) ) { // debug
            //   throw new RuntimeException("deg(A)+deg(B) > deg(C)");
            //}

            // prepare for next iteration
            Mq = Qi;
            Qi = Q.getIntegerModul().multiply(P.getIntegerModul());
            // Q = new ModIntegerRing(Qi.getVal());
            if ( ModLongRing.MAX_LONG.compareTo( Qi.getVal() ) > 0 ) {
                Q = (ModularRingFactory) new ModLongRing(Qi.getVal());
            } else {
                Q = (ModularRingFactory) new ModIntegerRing(Qi.getVal());
            }
            Ai = Ea;
            Bi = Eb;
        }
        GreatestCommonDivisorAbstract<BigInteger> ufd = new GreatestCommonDivisorPrimitive<BigInteger>();

        // remove normalization
        BigInteger ai = ufd.baseContent(Ai);
        Ai = Ai.divide(ai);
        BigInteger bi = null;
        try {
            bi = c.divide(ai);
            Bi = Bi.divide(bi); // divide( c/a )
        } catch (RuntimeException e) {
            //System.out.println("C  = " + C );
            //System.out.println("Ai = " + Ai );
            //System.out.println("Bi = " + Bi );
            //System.out.println("c  = " + c );
            //System.out.println("ai = " + ai );
            //System.out.println("bi = " + bi );
            //System.out.println("no exact lifting possible " + e);
            throw new NoLiftingException("no exact lifting possible " +e);
        }
        return new HenselApprox<MOD>(Ai,Bi,A1p,B1p);
    }


    /**
     * Modular Hensel lifting algorithm on coefficients. Let p =
     * A.ring.coFac.modul() = B.ring.coFac.modul() and assume C == A*B mod p
     * with ggt(A,B) == 1 mod p. See algorithm 6.1. in Geddes et.al. and
     * algorithms 3.5.{5,6} in Cohen. 
     * @param C GenPolynomial
     * @param A GenPolynomial
     * @param B other GenPolynomial
     * @param M bound on the coefficients of A1 and B1 as factors of C.
     * @return [A1,B1] = lift(C,A,B), with C = A1 * B1.
     */
    @SuppressWarnings("unchecked")
    public static <MOD extends GcdRingElem<MOD> & Modular>
      HenselApprox<MOD> liftHensel(GenPolynomial<BigInteger> C, BigInteger M, GenPolynomial<MOD> A, GenPolynomial<MOD> B) throws NoLiftingException {
        if (C == null || C.isZERO()) {
            return new HenselApprox<MOD>(C,C,A,B);
        }
        if (A == null || A.isZERO() || B == null || B.isZERO()) {
            throw new RuntimeException("A and B must be nonzero");
        }
        GenPolynomialRing<BigInteger> fac = C.ring;
        if (fac.nvar != 1) { // todo assert
            throw new RuntimeException("polynomial ring not univariate");
        }
        // one Hensel step on part polynomials
        GenPolynomial<MOD>[] gst = A.egcd(B);
        if (!gst[0].isONE()) {
            throw new NoLiftingException("A and B not coprime, gcd = " + gst[0] + ", A = " + A + ", B = " + B);
        }
        GenPolynomial<MOD> s = gst[1];
        GenPolynomial<MOD> t = gst[2];
        HenselApprox<MOD> ab = HenselUtil.<MOD> liftHensel(C, M, A, B, s, t);
        return ab;
    }


    /**
     * Modular Hensel lifting algorithm on coefficients. Let p =
     * f_i.ring.coFac.modul() i = 0, ..., n-1 and assume C == prod_{0,...,n-1}
     * f_i mod p with ggt(f_i,f_j) == 1 mod p for i != j
     * @param C GenPolynomial
     * @param F = [f_0,...,f_{n-1}] list of GenPolynomial
     * @param M bound on the coefficients of g_i as factors of C.
     * @return [g_0,...,g_{n-1}] = lift(C,F), with C = prod_{0,...,n-1} g_i mod
     *         p^e.
     */
    public static <MOD extends GcdRingElem<MOD> & Modular>
      List<GenPolynomial<BigInteger>> liftHensel(GenPolynomial<BigInteger> C, BigInteger M, List<GenPolynomial<MOD>> F) throws NoLiftingException {
        if (C == null || C.isZERO() || F == null || F.size() == 0) {
            throw new RuntimeException("C must be nonzero and F must be nonempty");
        }
        GenPolynomialRing<BigInteger> fac = C.ring;
        if (fac.nvar != 1) { // todo assert
            throw new RuntimeException("polynomial ring not univariate");
        }
        List<GenPolynomial<BigInteger>> lift = new ArrayList<GenPolynomial<BigInteger>>(F.size());
        GenPolynomial<MOD> cnst = null;
        GenPolynomial<BigInteger> icnst = null;
        for (GenPolynomial<MOD> ct : F) {
            if (ct.isConstant()) {
                if (cnst == null) {
                    cnst = ct;
                } else {
                    throw new RuntimeException("more than one constant " + cnst + ", " + ct);
                }
            }
        }
        if (cnst != null) {
            F.remove(cnst);
            BigInteger ilc = cnst.leadingBaseCoefficient().getSymmetricInteger();
            icnst = fac.getONE().multiply(ilc);
        } else {
            // cnst = F.get(0).ring.getONE();
        }
        int n = F.size();
        if (n == 1) { // use C itself
            lift.add(C);
            return lift;
        }
        if (n == 2) { // only one step
            if (icnst != null) {
                lift.add(icnst);
            }
            HenselApprox<MOD> ab = HenselUtil.<MOD> liftHensel(C, M, F.get(0), F.get(1));
            lift.add(ab.A);
            lift.add(ab.B);
            return lift;
        }
        BigInteger lc = C.leadingBaseCoefficient();
        GenPolynomial<MOD> f = F.get(0);
        GenPolynomialRing<MOD> mfac = f.ring;
        ModularRingFactory<MOD> mr = (ModularRingFactory<MOD>) mfac.coFac;
        BigInteger P = mr.getIntegerModul();
        // split list in two parts and prepare polynomials
        int k = n / 2;
        List<GenPolynomial<MOD>> F1 = new ArrayList<GenPolynomial<MOD>>(k);
        GenPolynomial<MOD> A = mfac.getONE();
        MOD mlc = mr.fromInteger(lc.getVal());
        A = A.multiply(mlc);
        for (int i = 0; i < k; i++) {
            GenPolynomial<MOD> fi = F.get(i);
            if (fi != null && !fi.isZERO()) {
                A = A.multiply(fi);
                F1.add(fi);
                //} else {
                //System.out.println("A = " + A + ", fi = " + fi);
            }
        }
        //System.out.println("A = " + A);
        List<GenPolynomial<MOD>> F2 = new ArrayList<GenPolynomial<MOD>>(k);
        GenPolynomial<MOD> B = mfac.getONE();
        for (int i = k; i < n; i++) {
            GenPolynomial<MOD> fi = F.get(i);
            if (fi != null && !fi.isZERO()) {
                B = B.multiply(fi);
                F2.add(fi);
                //} else {
                //System.out.println("B = " + B + ", fi = " + fi);
            }
        }
        //System.out.println("B = " + B);
        //System.out.println("A = " + A + ", F1 = " + F1);
        //System.out.println("B = " + B + ", F2 = " + F2);
        // one Hensel step on part polynomials
        HenselApprox<MOD> ab = HenselUtil.<MOD> liftHensel(C, M, A, B);
        GenPolynomial<BigInteger> A1 = ab.A;
        GenPolynomial<BigInteger> B1 = ab.B;
        if (!isHenselLift(C, M, P, A1, B1)) {
            throw new NoLiftingException("no lifting A1, B1");
        }
        //System.out.println("Am = " + ab.Am + ", Bm = " + ab.Bm);
        //System.out.println("A1 = " + A1 + ", B1 = " + B1);
        BigInteger Mh = M.divide(new BigInteger(2));
        // recursion on list parts
        List<GenPolynomial<BigInteger>> G1 = HenselUtil.<MOD> liftHensel(A1, Mh, F1);
        if (!isHenselLift(A1, Mh, P, G1)) {
            throw new NoLiftingException("no lifting G1");
        }
        //System.out.println("A1 = " + A1 + ", F1 = " + F1);
        //System.out.println("G1 = " + G1);
        List<GenPolynomial<BigInteger>> G2 = HenselUtil.<MOD> liftHensel(B1, Mh, F2);
        if (!isHenselLift(B1, Mh, P, G2)) {
            throw new NoLiftingException("no lifting G2");
        }
        //System.out.println("B1 = " + B1 + ", F2 = " + F2);
        //System.out.println("G2 = " + G2);
        if (icnst != null) {
            lift.add(icnst);
        }
        lift.addAll(G1);
        lift.addAll(G2);
        return lift;
    }


    /**
     * Modular quadratic Hensel lifting algorithm on coefficients. Let p =
     * A.ring.coFac.modul() = B.ring.coFac.modul() and assume C == A*B mod p
     * with ggt(A,B) == 1 mod p and S A + T B == 1 mod p. See algorithm 6.1. in
     * Geddes et.al. and algorithms 3.5.{5,6} in Cohen. Quadratic version, as it
     * also lifts S A + T B == 1 mod p^{e+1}.
     * @param C GenPolynomial
     * @param A GenPolynomial
     * @param B other GenPolynomial
     * @param S GenPolynomial
     * @param T GenPolynomial
     * @param M bound on the coefficients of A1 and B1 as factors of C.
     * @return [A1,B1] = lift(C,A,B), with C = A1 * B1.
     */
    @SuppressWarnings("unchecked")
    public static <MOD extends GcdRingElem<MOD> & Modular>
      HenselApprox<MOD> liftHenselQuadratic(GenPolynomial<BigInteger> C, BigInteger M,
                                            GenPolynomial<MOD> A, GenPolynomial<MOD> B, 
                                            GenPolynomial<MOD> S, GenPolynomial<MOD> T) throws NoLiftingException {
        if (C == null || C.isZERO()) {
            return new HenselApprox<MOD>(C,C,A,B);
        }
        if (A == null || A.isZERO() || B == null || B.isZERO()) {
            throw new RuntimeException("A and B must be nonzero");
        }
        GenPolynomialRing<BigInteger> fac = C.ring;
        if (fac.nvar != 1) { // todo assert
            throw new RuntimeException("polynomial ring not univariate");
        }
        // setup factories
        GenPolynomialRing<MOD> pfac = A.ring;
        RingFactory<MOD> p = pfac.coFac;
        RingFactory<MOD> q = p;
        ModularRingFactory<MOD> P = (ModularRingFactory<MOD>) p;
        ModularRingFactory<MOD> Q = (ModularRingFactory<MOD>) q;
        BigInteger Qi = Q.getIntegerModul();
        BigInteger M2 = M.multiply(M.fromInteger(2));
        BigInteger Mq = Qi;
        GenPolynomialRing<MOD> qfac;
        qfac = new GenPolynomialRing<MOD>(Q, pfac);

        // normalize c and a, b factors, assert p is prime
        GenPolynomial<BigInteger> Ai;
        GenPolynomial<BigInteger> Bi;
        BigInteger c = C.leadingBaseCoefficient();
        C = C.multiply(c); // sic
        MOD a = A.leadingBaseCoefficient();
        if (!a.isONE()) { // A = A.monic();
            A = A.divide(a);
            S = S.multiply(a);
        }
        MOD b = B.leadingBaseCoefficient();
        if (!b.isONE()) { // B = B.monic();
            B = B.divide(b);
            T = T.multiply(b);
        }
        MOD ci = P.fromInteger(c.getVal());
        A = A.multiply(ci);
        B = B.multiply(ci);
        T = T.divide(ci);
        S = S.divide(ci);
        Ai = PolyUtil.integerFromModularCoefficients(fac, A);
        Bi = PolyUtil.integerFromModularCoefficients(fac, B);
        // replace leading base coefficients
        ExpVector ea = Ai.leadingExpVector();
        ExpVector eb = Bi.leadingExpVector();
        Ai.doPutToMap(ea, c);
        Bi.doPutToMap(eb, c);

        // polynomials mod p
        GenPolynomial<MOD> Ap;
        GenPolynomial<MOD> Bp;
        GenPolynomial<MOD> A1p = A;
        GenPolynomial<MOD> B1p = B;
        GenPolynomial<MOD> Ep;
        GenPolynomial<MOD> Sp = S;
        GenPolynomial<MOD> Tp = T;

        // polynomials mod q
        GenPolynomial<MOD> Aq;
        GenPolynomial<MOD> Bq;
        GenPolynomial<MOD> Eq;

        // polynomials over the integers
        GenPolynomial<BigInteger> E;
        GenPolynomial<BigInteger> Ea;
        GenPolynomial<BigInteger> Eb;
        GenPolynomial<BigInteger> Ea1;
        GenPolynomial<BigInteger> Eb1;
        GenPolynomial<BigInteger> Si;
        GenPolynomial<BigInteger> Ti;

        Si = PolyUtil.integerFromModularCoefficients(fac, S);
        Ti = PolyUtil.integerFromModularCoefficients(fac, T);

        Aq = PolyUtil.<MOD> fromIntegerCoefficients(qfac, Ai);
        Bq = PolyUtil.<MOD> fromIntegerCoefficients(qfac, Bi);

        while (Mq.compareTo(M2) < 0) {
            // compute E=(C-AB)/q over the integers
            E = C.subtract(Ai.multiply(Bi));
            if (E.isZERO()) {
                //System.out.println("leaving on zero error");
                logger.info("leaving on zero error");
                break;
            }
            E = E.divide(Qi);
            // E mod p
            Ep = PolyUtil.<MOD> fromIntegerCoefficients(qfac, E);
            //logger.info("Ep = " + Ep + ", qfac = " + qfac);
            if (Ep.isZERO()) {
                //System.out.println("leaving on zero error");
                //??logger.info("leaving on zero error Ep");
                //??break;
            }

            // construct approximation mod p
            Ap = Sp.multiply(Ep); // S,T ++ T,S
            Bp = Tp.multiply(Ep);
            GenPolynomial<MOD>[] QR;
            //logger.info("Ap = " + Ap + ", Bp = " + Bp + ", fac(Ap) = " + Ap.ring);
            QR = Ap.divideAndRemainder(Bq);
            GenPolynomial<MOD> Qp;
            GenPolynomial<MOD> Rp;
            Qp = QR[0];
            Rp = QR[1];
            //logger.info("Qp = " + Qp + ", Rp = " + Rp);
            A1p = Rp;
            B1p = Bp.sum(Aq.multiply(Qp));

            // construct q-adic approximation, convert to integer
            Ea = PolyUtil.integerFromModularCoefficients(fac, A1p);
            Eb = PolyUtil.integerFromModularCoefficients(fac, B1p);
            Ea1 = Ea.multiply(Qi);
            Eb1 = Eb.multiply(Qi);
            Ea = Ai.sum(Eb1); // Eb1 and Ea1 are required
            Eb = Bi.sum(Ea1); //--------------------------
            assert (Ea.degree(0) + Eb.degree(0) <= C.degree(0));
            //if ( Ea.degree(0)+Eb.degree(0) > C.degree(0) ) { // debug
            //   throw new RuntimeException("deg(A)+deg(B) > deg(C)");
            //}
            Ai = Ea;
            Bi = Eb;

            // gcd representation factors error --------------------------------
            // compute E=(1-SA-TB)/q over the integers
            E = fac.getONE();
            E = E.subtract(Si.multiply(Ai)).subtract(Ti.multiply(Bi));
            E = E.divide(Qi);
            // E mod q
            Ep = PolyUtil.<MOD> fromIntegerCoefficients(qfac, E);
            //logger.info("Ep2 = " + Ep);

            // construct approximation mod q
            Ap = Sp.multiply(Ep); // S,T ++ T,S
            Bp = Tp.multiply(Ep);
            QR = Bp.divideAndRemainder(Aq); // Ai == A mod p ?
            Qp = QR[0];
            Rp = QR[1];
            B1p = Rp;
            A1p = Ap.sum(Bq.multiply(Qp));

            if (false && debug) {
                Eq = A1p.multiply(Aq).sum(B1p.multiply(Bq)).subtract(Ep);
                if (!Eq.isZERO()) {
                    System.out.println("A*A1p+B*B1p-Ep2 != 0 " + Eq);
                    throw new RuntimeException("A*A1p+B*B1p-Ep2 != 0 mod " + Q.getIntegerModul());
                }
            }

            // construct q-adic approximation, convert to integer
            Ea = PolyUtil.integerFromModularCoefficients(fac, A1p);
            Eb = PolyUtil.integerFromModularCoefficients(fac, B1p);
            Ea1 = Ea.multiply(Qi);
            Eb1 = Eb.multiply(Qi);
            Ea = Si.sum(Ea1); // Eb1 and Ea1 are required
            Eb = Ti.sum(Eb1); //--------------------------
            Si = Ea;
            Ti = Eb;

            // prepare for next iteration
            Mq = Qi;
            Qi = Q.getIntegerModul().multiply(Q.getIntegerModul());
            if ( ModLongRing.MAX_LONG.compareTo( Qi.getVal() ) > 0 ) {
                Q = (ModularRingFactory) new ModLongRing(Qi.getVal());
            } else {
                Q = (ModularRingFactory) new ModIntegerRing(Qi.getVal());
            }
            //Q = new ModIntegerRing(Qi.getVal());
            //System.out.println("Q = " + Q + ", from Q = " + Mq);

            qfac = new GenPolynomialRing<MOD>(Q, pfac);

            Aq = PolyUtil.<MOD> fromIntegerCoefficients(qfac, Ai);
            Bq = PolyUtil.<MOD> fromIntegerCoefficients(qfac, Bi);
            Sp = PolyUtil.<MOD> fromIntegerCoefficients(qfac, Si);
            Tp = PolyUtil.<MOD> fromIntegerCoefficients(qfac, Ti);
            if (false && debug) {
                E = Ai.multiply(Si).sum(Bi.multiply(Ti));
                Eq = PolyUtil.<MOD> fromIntegerCoefficients(qfac, E);
                if (!Eq.isONE()) {
                    System.out.println("Ai*Si+Bi*Ti=1 " + Eq);
                    throw new RuntimeException("Ai*Si+Bi*Ti != 1 mod " + Q.getIntegerModul());
                }
            }
        }
        GreatestCommonDivisorAbstract<BigInteger> ufd = new GreatestCommonDivisorPrimitive<BigInteger>();

        // remove normalization if possible
        BigInteger ai = ufd.baseContent(Ai);
        Ai = Ai.divide(ai);
        BigInteger bi = null;
        try {
            bi = c.divide(ai);
            Bi = Bi.divide(bi); // divide( c/a )
        } catch (RuntimeException e) {
            //System.out.println("C  = " + C );
            //System.out.println("Ai = " + Ai );
            //System.out.println("Bi = " + Bi );
            //System.out.println("c  = " + c );
            //System.out.println("ai = " + ai );
            //System.out.println("bi = " + bi );
            //System.out.println("no exact lifting possible " + e);
            throw new NoLiftingException("no exact lifting possible " +e);
        }
        return new HenselApprox<MOD>(Ai,Bi,A1p,B1p);
    }


    /**
     * Modular quadratic Hensel lifting algorithm on coefficients. Let p =
     * A.ring.coFac.modul() = B.ring.coFac.modul() and assume C == A*B mod p
     * with ggt(A,B) == 1 mod p. See algorithm 6.1. in Geddes et.al. and
     * algorithms 3.5.{5,6} in Cohen. Quadratic version.
     * @param C GenPolynomial
     * @param A GenPolynomial
     * @param B other GenPolynomial
     * @param M bound on the coefficients of A1 and B1 as factors of C.
     * @return [A1,B1] = lift(C,A,B), with C = A1 * B1.
     */
    @SuppressWarnings("unchecked")
    public static <MOD extends GcdRingElem<MOD> & Modular>
      HenselApprox<MOD> liftHenselQuadratic(GenPolynomial<BigInteger> C, BigInteger M, 
                                            GenPolynomial<MOD> A, GenPolynomial<MOD> B) throws NoLiftingException {
        if (C == null || C.isZERO()) {
            return new HenselApprox<MOD>(C,C,A,B);
        }
        if (A == null || A.isZERO() || B == null || B.isZERO()) {
            throw new RuntimeException("A and B must be nonzero");
        }
        GenPolynomialRing<BigInteger> fac = C.ring;
        if (fac.nvar != 1) { // todo assert
            throw new RuntimeException("polynomial ring not univariate");
        }
        // one Hensel step on part polynomials
        GenPolynomial<MOD>[] gst = A.egcd(B);
        if (!gst[0].isONE()) {
            throw new NoLiftingException("A and B not coprime, gcd = " + gst[0] + ", A = " + A + ", B = " + B);
        }
        GenPolynomial<MOD> s = gst[1];
        GenPolynomial<MOD> t = gst[2];
        HenselApprox<MOD> ab = HenselUtil.<MOD> liftHenselQuadratic(C, M, A, B, s, t);
        return ab;
    }


    /**
     * Modular quadratic Hensel lifting algorithm on coefficients. Let p =
     * f_i.ring.coFac.modul() i = 0, ..., n-1 and assume C == prod_{0,...,n-1}
     * f_i mod p with ggt(f_i,f_j) == 1 mod p for i != j
     * @param C GenPolynomial
     * @param F = [f_0,...,f_{n-1}] list of GenPolynomial
     * @param M bound on the coefficients of g_i as factors of C.
     * @return [g_0,...,g_{n-1}] = lift(C,F), with C = prod_{0,...,n-1} g_i mod
     *         p**e.
     */
    public static <MOD extends GcdRingElem<MOD> & Modular>
      List<GenPolynomial<BigInteger>> liftHenselQuadratic(GenPolynomial<BigInteger> C, BigInteger M, List<GenPolynomial<MOD>> F) throws NoLiftingException {
        if (C == null || C.isZERO() || F == null || F.size() == 0) {
            throw new RuntimeException("C must be nonzero and F must be nonempty");
        }
        GenPolynomialRing<BigInteger> fac = C.ring;
        if (fac.nvar != 1) { // todo assert
            throw new RuntimeException("polynomial ring not univariate");
        }
        List<GenPolynomial<BigInteger>> lift = new ArrayList<GenPolynomial<BigInteger>>(F.size());
        GenPolynomial<MOD> cnst = null;
        GenPolynomial<BigInteger> icnst = null;
        for (GenPolynomial<MOD> ct : F) {
            if (ct.isConstant()) {
                if (cnst == null) {
                    cnst = ct;
                } else {
                    throw new RuntimeException("more than one constant " + cnst + ", " + ct);
                }
            }
        }
        if (cnst != null) {
            F.remove(cnst);
            BigInteger ilc = cnst.leadingBaseCoefficient().getSymmetricInteger();
            icnst = fac.getONE().multiply(ilc);
        } else {
            // cnst = F.get(0).ring.getONE();
        }
        int n = F.size();
        if (n == 1) { // use C itself
            lift.add(C);
            return lift;
        }
        if (n == 2) { // only one step
            if (icnst != null) {
                lift.add(icnst);
            }
            HenselApprox<MOD> ab = HenselUtil.<MOD> liftHenselQuadraticFac(C, M, F.get(0), F.get(1));
            lift.add(ab.A);
            lift.add(ab.B);
            return lift;
        }
        BigInteger lc = C.leadingBaseCoefficient();
        GenPolynomial<MOD> f = F.get(0);
        GenPolynomialRing<MOD> mfac = f.ring;
        ModularRingFactory<MOD> mr = (ModularRingFactory<MOD>) mfac.coFac;
        BigInteger P = mr.getIntegerModul();
        // split list in two parts and prepare polynomials
        int k = n / 2;
        List<GenPolynomial<MOD>> F1 = new ArrayList<GenPolynomial<MOD>>(k);
        GenPolynomial<MOD> A = mfac.getONE();
        MOD mlc = mr.fromInteger(lc.getVal());
        A = A.multiply(mlc);
        for (int i = 0; i < k; i++) {
            GenPolynomial<MOD> fi = F.get(i);
            if (fi != null && !fi.isZERO()) {
                A = A.multiply(fi);
                F1.add(fi);
                //} else {
                //System.out.println("A = " + A + ", fi = " + fi);
            }
        }
        List<GenPolynomial<MOD>> F2 = new ArrayList<GenPolynomial<MOD>>(k);
        GenPolynomial<MOD> B = mfac.getONE();
        for (int i = k; i < n; i++) {
            GenPolynomial<MOD> fi = F.get(i);
            if (fi != null && !fi.isZERO()) {
                B = B.multiply(fi);
                F2.add(fi);
                //} else {
                //System.out.println("B = " + B + ", fi = " + fi);
            }
        }
        // one Hensel step on part polynomials
        HenselApprox<MOD> ab = HenselUtil.<MOD> liftHenselQuadraticFac(C, M, A, B);
        GenPolynomial<BigInteger> A1 = ab.A;
        GenPolynomial<BigInteger> B1 = ab.B;
        if (!isHenselLift(C, M, P, A1, B1)) {
            System.out.println("A = " + A + ", F1 = " + F1);
            System.out.println("B = " + B + ", F2 = " + F2);
            System.out.println("A1 = " + A1 + ", B1 = " + B1);
            throw new NoLiftingException("no lifting A1, B1");
        }
        BigInteger Mh = M.divide(new BigInteger(2));
        // recursion on list parts
        List<GenPolynomial<BigInteger>> G1 = HenselUtil.<MOD> liftHenselQuadratic(A1, Mh, F1);
        if (!isHenselLift(A1, Mh, P, G1)) {
            System.out.println("A = " + A + ", F1 = " + F1);
            System.out.println("B = " + B + ", F2 = " + F2);
            System.out.println("G1 = " + G1);
            throw new NoLiftingException("no lifting G1");
        }
        List<GenPolynomial<BigInteger>> G2 = HenselUtil.<MOD> liftHenselQuadratic(B1, Mh, F2);
        if (!isHenselLift(B1, Mh, P, G2)) {
            System.out.println("A = " + A + ", F1 = " + F1);
            System.out.println("B = " + B + ", F2 = " + F2);
            System.out.println("G2 = " + G2);
            throw new NoLiftingException("no lifting G2");
        }
        if (icnst != null) {
            lift.add(icnst);
        }
        lift.addAll(G1);
        lift.addAll(G2);
        return lift;
    }


    /**
     * Modular Hensel lifting algorithm on coefficients. Let p =
     * A.ring.coFac.modul() = B.ring.coFac.modul() and assume C == A*B mod p
     * with ggt(A,B) == 1 mod p. See algorithm 6.1. in Geddes et.al. and
     * algorithms 3.5.{5,6} in Cohen. Quadratic version.
     * @param C GenPolynomial
     * @param A GenPolynomial
     * @param B other GenPolynomial
     * @param M bound on the coefficients of A1 and B1 as factors of C.
     * @return [A1,B1] = lift(C,A,B), with C = A1 * B1.
     */
    @SuppressWarnings("unchecked")
    public static <MOD extends GcdRingElem<MOD> & Modular>
      HenselApprox<MOD> liftHenselQuadraticFac(GenPolynomial<BigInteger> C, BigInteger M,
                                               GenPolynomial<MOD> A, GenPolynomial<MOD> B) throws NoLiftingException {
        if (C == null || C.isZERO()) {
            throw new RuntimeException("C must be nonzero");
        }
        if (A == null || A.isZERO() || B == null || B.isZERO()) {
            throw new RuntimeException("A and B must be nonzero");
        }
        GenPolynomialRing<BigInteger> fac = C.ring;
        if (fac.nvar != 1) { // todo assert
            throw new RuntimeException("polynomial ring not univariate");
        }
        // one Hensel step on part polynomials
        GenPolynomial<MOD>[] gst = A.egcd(B);
        if (!gst[0].isONE()) {
            throw new NoLiftingException("A and B not coprime, gcd = " + gst[0] + ", A = " + A + ", B = " + B);
        }
        GenPolynomial<MOD> s = gst[1];
        GenPolynomial<MOD> t = gst[2];
        HenselApprox<MOD> ab = HenselUtil.<MOD> liftHenselQuadraticFac(C, M, A, B, s, t);
        return ab;
    }


    /**
     * Modular Hensel lifting algorithm on coefficients. Let p =
     * A.ring.coFac.modul() = B.ring.coFac.modul() and assume C == A*B mod p
     * with ggt(A,B) == 1 mod p and S A + T B == 1 mod p. See algorithm 6.1. in
     * Geddes et.al. and algorithms 3.5.{5,6} in Cohen. Quadratic version, as it
     * also lifts S A + T B == 1 mod p^{e+1}.
     * @param C primitive GenPolynomial
     * @param A GenPolynomial
     * @param B other GenPolynomial
     * @param S GenPolynomial
     * @param T GenPolynomial
     * @param M bound on the coefficients of A1 and B1 as factors of C.
     * @return [A1,B1] = lift(C,A,B), with C = A1 * B1.
     */
    @SuppressWarnings("unchecked")
    public static <MOD extends GcdRingElem<MOD> & Modular>
      HenselApprox<MOD> liftHenselQuadraticFac(GenPolynomial<BigInteger> C, BigInteger M,
                                               GenPolynomial<MOD> A, GenPolynomial<MOD> B, 
                                               GenPolynomial<MOD> S, GenPolynomial<MOD> T) throws NoLiftingException {
        //System.out.println("*** version for factorization *** ");
        GenPolynomial<BigInteger>[] AB = (GenPolynomial<BigInteger>[]) new GenPolynomial[2];
        if (C == null || C.isZERO()) {
            throw new RuntimeException("C must be nonzero");
        }
        if (A == null || A.isZERO() || B == null || B.isZERO()) {
            throw new RuntimeException("A and B must be nonzero");
        }
        GenPolynomialRing<BigInteger> fac = C.ring;
        if (fac.nvar != 1) { // todo assert
            throw new RuntimeException("polynomial ring not univariate");
        }
        // setup factories
        GenPolynomialRing<MOD> pfac = A.ring;
        RingFactory<MOD> p = pfac.coFac;
        RingFactory<MOD> q = p;
        ModularRingFactory<MOD> P = (ModularRingFactory<MOD>) p;
        ModularRingFactory<MOD> Q = (ModularRingFactory<MOD>) q;
        BigInteger PP = Q.getIntegerModul();
        BigInteger Qi = PP;
        BigInteger M2 = M.multiply(M.fromInteger(2));
        if (debug) {
            //System.out.println("M2 =  " + M2);
            logger.debug("M2 =  " + M2);
        }
        BigInteger Mq = Qi;
        GenPolynomialRing<MOD> qfac;
        qfac = new GenPolynomialRing<MOD>(Q, pfac); // mod p
        GenPolynomialRing<MOD> mfac;
        BigInteger Mi = Q.getIntegerModul().multiply(Q.getIntegerModul());
        ModularRingFactory<MOD> Qmm;
        // = new ModIntegerRing(Mi.getVal());
        if ( ModLongRing.MAX_LONG.compareTo( Mi.getVal() ) > 0 ) {
            Qmm = (ModularRingFactory) new ModLongRing(Mi.getVal());
        } else {
            Qmm = (ModularRingFactory) new ModIntegerRing(Mi.getVal());
        }
        mfac = new GenPolynomialRing<MOD>(Qmm, qfac); // mod p^e
        MOD Qm = Qmm.fromInteger(Qi.getVal());

        // partly normalize c and a, b factors, assert p is prime
        GenPolynomial<BigInteger> Ai;
        GenPolynomial<BigInteger> Bi;
        BigInteger c = C.leadingBaseCoefficient();
        C = C.multiply(c); // sic
        MOD a = A.leadingBaseCoefficient();
        if (!a.isONE()) { // A = A.monic();
            A = A.divide(a);
            S = S.multiply(a);
        }
        MOD b = B.leadingBaseCoefficient();
        if (!b.isONE()) { // B = B.monic();
            B = B.divide(b);
            T = T.multiply(b);
        }
        MOD ci = P.fromInteger(c.getVal());
        if (ci.isZERO()) {
            System.out.println("c =  " + c);
            System.out.println("P =  " + P);
            throw new RuntimeException("c mod p == 0 not meaningful");
        }
        // mod p
        A = A.multiply(ci);
        S = S.divide(ci);
        B = B.multiply(ci);
        T = T.divide(ci);
        Ai = PolyUtil.integerFromModularCoefficients(fac, A);
        Bi = PolyUtil.integerFromModularCoefficients(fac, B);
        // replace leading base coefficients
        ExpVector ea = Ai.leadingExpVector();
        ExpVector eb = Bi.leadingExpVector();
        Ai.doPutToMap(ea, c);
        Bi.doPutToMap(eb, c);

        // polynomials mod p
        GenPolynomial<MOD> Ap;
        GenPolynomial<MOD> Bp;
        GenPolynomial<MOD> A1p = A;
        GenPolynomial<MOD> B1p = B;
        GenPolynomial<MOD> Sp = S;
        GenPolynomial<MOD> Tp = T;

        // polynomials mod q
        GenPolynomial<MOD> Aq;
        GenPolynomial<MOD> Bq;

        // polynomials mod p^e
        GenPolynomial<MOD> Cm;
        GenPolynomial<MOD> Am;
        GenPolynomial<MOD> Bm;
        GenPolynomial<MOD> Em;
        GenPolynomial<MOD> Emp;
        GenPolynomial<MOD> Sm;
        GenPolynomial<MOD> Tm;
        GenPolynomial<MOD> Ema;
        GenPolynomial<MOD> Emb;
        GenPolynomial<MOD> Ema1;
        GenPolynomial<MOD> Emb1;

        // polynomials over the integers
        GenPolynomial<BigInteger> Ei;
        GenPolynomial<BigInteger> Si;
        GenPolynomial<BigInteger> Ti;

        Si = PolyUtil.integerFromModularCoefficients(fac, S);
        Ti = PolyUtil.integerFromModularCoefficients(fac, T);

        Aq = PolyUtil.<MOD> fromIntegerCoefficients(qfac, Ai);
        Bq = PolyUtil.<MOD> fromIntegerCoefficients(qfac, Bi);

        // polynomials mod p^e
        Cm = PolyUtil.<MOD> fromIntegerCoefficients(mfac, C);
        Am = PolyUtil.<MOD> fromIntegerCoefficients(mfac, Ai);
        Bm = PolyUtil.<MOD> fromIntegerCoefficients(mfac, Bi);
        Sm = PolyUtil.<MOD> fromIntegerCoefficients(mfac, Si);
        Tm = PolyUtil.<MOD> fromIntegerCoefficients(mfac, Ti);
        //System.out.println("Cm =  " + Cm);
        //System.out.println("Am =  " + Am);
        //System.out.println("Bm =  " + Bm);
        //System.out.println("Ai =  " + Ai);
        //System.out.println("Bi =  " + Bi);
        //System.out.println("mfac =  " + mfac);

        while (Mq.compareTo(M2) < 0) {
            // compute E=(C-AB)/p mod p^e
            if (debug) {
                //System.out.println("mfac =  " + Cm.ring);
                logger.debug("mfac =  " + Cm.ring);
            }
            Em = Cm.subtract(Am.multiply(Bm));
            //System.out.println("Em =  " + Em);
            if (Em.isZERO()) {
                if (C.subtract(Ai.multiply(Bi)).isZERO()) {
                    //System.out.println("leaving on zero error");
                    logger.info("leaving on zero error");
                    break;
                }
            }
            // Em = Em.divide( Qm );
            Ei = PolyUtil.integerFromModularCoefficients(fac, Em);
            Ei = Ei.divide(Qi);
            //System.out.println("Ei =  " + Ei);

            // Ei mod p
            Emp = PolyUtil.<MOD> fromIntegerCoefficients(qfac, Ei);
            //            Emp = PolyUtil.<MOD>fromIntegerCoefficients(qfac,
            //               PolyUtil.integerFromModularCoefficients(fac,Em) ); 
            //System.out.println("Emp =  " + Emp);
            //logger.info("Emp = " + Emp);
            if (Emp.isZERO()) {
                if (C.subtract(Ai.multiply(Bi)).isZERO()) {
                    //System.out.println("leaving on zero error Emp");
                    logger.info("leaving on zero error Emp");
                    break;
                }
            }

            // construct approximation mod p
            Ap = Sp.multiply(Emp); // S,T ++ T,S 
            Bp = Tp.multiply(Emp);
            GenPolynomial<MOD>[] QR = null;
            QR = Ap.divideAndRemainder(Bq); // Bq !
            GenPolynomial<MOD> Qp = QR[0];
            GenPolynomial<MOD> Rp = QR[1];
            A1p = Rp;
            B1p = Bp.sum(Aq.multiply(Qp)); // Aq !
            //System.out.println("A1p =  " + A1p);
            //System.out.println("B1p =  " + B1p);

            // construct q-adic approximation
            Ema = PolyUtil.<MOD> fromIntegerCoefficients(mfac, PolyUtil.integerFromModularCoefficients(fac, A1p));
            Emb = PolyUtil.<MOD> fromIntegerCoefficients(mfac, PolyUtil.integerFromModularCoefficients(fac, B1p));
            //System.out.println("Ema =  " + Ema);
            //System.out.println("Emb =  " + Emb);
            Ema1 = Ema.multiply(Qm);
            Emb1 = Emb.multiply(Qm);
            Ema = Am.sum(Emb1); // Eb1 and Ea1 are required
            Emb = Bm.sum(Ema1); //--------------------------
            assert (Ema.degree(0) + Emb.degree(0) <= Cm.degree(0));
            //if ( Ema.degree(0)+Emb.degree(0) > Cm.degree(0) ) { // debug
            //   throw new RuntimeException("deg(A)+deg(B) > deg(C)");
            //}
            Am = Ema;
            Bm = Emb;
            Ai = PolyUtil.integerFromModularCoefficients(fac, Am);
            Bi = PolyUtil.integerFromModularCoefficients(fac, Bm);
            //System.out.println("Am =  " + Am);
            //System.out.println("Bm =  " + Bm);
            //System.out.println("Ai =  " + Ai);
            //System.out.println("Bi =  " + Bi + "\n");

            // gcd representation factors error --------------------------------
            // compute E=(1-SA-TB)/p mod p^e
            Em = mfac.getONE();
            Em = Em.subtract(Sm.multiply(Am)).subtract(Tm.multiply(Bm));
            //System.out.println("Em  =  " + Em);
            // Em = Em.divide( Pm );

            Ei = PolyUtil.integerFromModularCoefficients(fac, Em);
            Ei = Ei.divide(Qi);
            //System.out.println("Ei =  " + Ei);
            // Ei mod p
            Emp = PolyUtil.<MOD> fromIntegerCoefficients(qfac, Ei);
            //Emp = PolyUtil.<MOD>fromIntegerCoefficients(qfac,
            //               PolyUtil.integerFromModularCoefficients(fac,Em) );
            //System.out.println("Emp =  " + Emp);

            // construct approximation mod p
            Ap = Sp.multiply(Emp); // S,T ++ T,S // Ep Eqp
            Bp = Tp.multiply(Emp); // Ep Eqp
            QR = Bp.divideAndRemainder(Aq); // Ap Aq ! // Ai == A mod p ?
            Qp = QR[0];
            Rp = QR[1];
            B1p = Rp;
            A1p = Ap.sum(Bq.multiply(Qp));
            //System.out.println("A1p =  " + A1p);
            //System.out.println("B1p =  " + B1p);

            // construct p^e-adic approximation
            Ema = PolyUtil.<MOD> fromIntegerCoefficients(mfac, PolyUtil.integerFromModularCoefficients(fac, A1p));
            Emb = PolyUtil.<MOD> fromIntegerCoefficients(mfac, PolyUtil.integerFromModularCoefficients(fac, B1p));
            Ema1 = Ema.multiply(Qm);
            Emb1 = Emb.multiply(Qm);
            Ema = Sm.sum(Ema1); // Emb1 and Ema1 are required
            Emb = Tm.sum(Emb1); //--------------------------
            Sm = Ema;
            Tm = Emb;
            Si = PolyUtil.integerFromModularCoefficients(fac, Sm);
            Ti = PolyUtil.integerFromModularCoefficients(fac, Tm);
            //System.out.println("Sm =  " + Sm);
            //System.out.println("Tm =  " + Tm);
            //System.out.println("Si =  " + Si);
            //System.out.println("Ti =  " + Ti + "\n");

            // prepare for next iteration
            Qi = Q.getIntegerModul().multiply(Q.getIntegerModul());
            Mq = Qi;
            //lmfac = mfac;
            // Q = new ModIntegerRing(Qi.getVal());
            if ( ModLongRing.MAX_LONG.compareTo( Qi.getVal() ) > 0 ) {
                Q = (ModularRingFactory) new ModLongRing(Qi.getVal());
            } else {
                Q = (ModularRingFactory) new ModIntegerRing(Qi.getVal());
            }
            qfac = new GenPolynomialRing<MOD>(Q, pfac);
            BigInteger Qmmi = Qmm.getIntegerModul().multiply(Qmm.getIntegerModul());
            //Qmm = new ModIntegerRing(Qmmi.getVal());
            if ( ModLongRing.MAX_LONG.compareTo( Qmmi.getVal() ) > 0 ) {
                Qmm = (ModularRingFactory) new ModLongRing(Qmmi.getVal());
            } else {
                Qmm = (ModularRingFactory) new ModIntegerRing(Qmmi.getVal());
            }
            mfac = new GenPolynomialRing<MOD>(Qmm, qfac);
            Qm = Qmm.fromInteger(Qi.getVal());

            Cm = PolyUtil.<MOD> fromIntegerCoefficients(mfac, C);
            Am = PolyUtil.<MOD> fromIntegerCoefficients(mfac, Ai);
            Bm = PolyUtil.<MOD> fromIntegerCoefficients(mfac, Bi);
            Sm = PolyUtil.<MOD> fromIntegerCoefficients(mfac, Si);
            Tm = PolyUtil.<MOD> fromIntegerCoefficients(mfac, Ti);

            assert (isHenselLift(C, Mi, PP, Ai, Bi));
            Mi = Mi.fromInteger(Qmm.getIntegerModul().getVal());

            Aq = PolyUtil.<MOD> fromIntegerCoefficients(qfac, Ai);
            Bq = PolyUtil.<MOD> fromIntegerCoefficients(qfac, Bi);
            Sp = PolyUtil.<MOD> fromIntegerCoefficients(qfac, Si);
            Tp = PolyUtil.<MOD> fromIntegerCoefficients(qfac, Ti);

            //System.out.println("Am =  " + Am);
            //System.out.println("Bm =  " + Bm);
            //System.out.println("Sm =  " + Sm);
            //System.out.println("Tm =  " + Tm);
            //System.out.println("mfac =  " + mfac);
            //System.out.println("Qmm = " + Qmm + ", M2   =  " + M2 + ", Mq   =  " + Mq + "\n");
        }
        //System.out.println("*Ai =  " + Ai);
        //System.out.println("*Bi =  " + Bi);

        Em = Cm.subtract(Am.multiply(Bm));
        if (!Em.isZERO()) {
            System.out.println("Em =  " + Em);
            //throw new NoLiftingException("no exact lifting possible");
        }
        // remove normalization not possible when not exact factorization
        GreatestCommonDivisorAbstract<BigInteger> ufd = new GreatestCommonDivisorPrimitive<BigInteger>();
        // remove normalization if possible
        BigInteger ai = ufd.baseContent(Ai);
        Ai = Ai.divide(ai); // Ai=pp(Ai)
        BigInteger[] qr = c.divideAndRemainder(ai);
        BigInteger bi = null;
        boolean exact = true;
        if (qr[1].isZERO()) {
            bi = qr[0];
            try {
                Bi = Bi.divide(bi); // divide( c/a )
            } catch (RuntimeException e) {
                System.out.println("*catch: no exact factorization: " + bi + ", e = " + e);
                exact = false;
            }
        } else {
            System.out.println("*remainder: no exact factorization: q = " + qr[0] + ", r = " + qr[1]);
            exact = false;
        }
        if (!exact) {
            System.out.println("*Ai =  " + Ai);
            System.out.println("*ai =  " + ai);
            System.out.println("*Bi =  " + Bi);
            System.out.println("*bi =  " + bi);
            System.out.println("*c  =  " + c);
            throw new NoLiftingException("no exact lifting possible");
        }
        return new HenselApprox<MOD>(Ai,Bi,Aq,Bq);
    }


    /**
     * Modular Hensel lifting algorithm on coefficients. Let p =
     * f_i.ring.coFac.modul() i = 0, ..., n-1 and assume C == prod_{0,...,n-1}
     * f_i mod p with ggt(f_i,f_j) == 1 mod p for i != j
     * @param C GenPolynomial
     * @param F = [f_0,...,f_{n-1}] list of GenPolynomial
     * @param M bound on the coefficients of g_i as factors of C.
     * @param D bit set of possible factor degrees.
     * @return [g_0,...,g_{n-1}] = lift(C,F), with C = prod_{0,...,n-1} g_i mod
     *         p^e.
     */
    public static <MOD extends GcdRingElem<MOD> & Modular>
      List<GenPolynomial<BigInteger>> liftHenselQuadraticFac(GenPolynomial<BigInteger> C, BigInteger M, List<GenPolynomial<MOD>> F, BitSet D) throws NoLiftingException {
        if (C == null || C.isZERO() || F == null || F.size() == 0) {
            throw new RuntimeException("C must be nonzero and F must be nonempty");
        }
        GenPolynomialRing<BigInteger> fac = C.ring;
        if (fac.nvar != 1) { // todo assert
            throw new RuntimeException("polynomial ring not univariate");
        }
        List<GenPolynomial<BigInteger>> lift = new ArrayList<GenPolynomial<BigInteger>>(F.size());
        GenPolynomial<MOD> cnst = null;
        GenPolynomial<BigInteger> icnst = null;
        for (GenPolynomial<MOD> ct : F) {
            if (ct.isConstant()) {
                if (cnst == null) {
                    cnst = ct;
                } else {
                    throw new RuntimeException("more than one constant " + cnst + ", " + ct);
                }
            }
        }
        if (cnst != null) {
            F.remove(cnst);
            BigInteger ilc = cnst.leadingBaseCoefficient().getSymmetricInteger();
            icnst = fac.getONE().multiply(ilc);
        } else {
            // cnst = F.get(0).ring.getONE();
        }
        int n = F.size();
        if (n == 1) { // use C itself
            lift.add(C);
            return lift;
        }
        if (n == 2) { // only one step
            if (icnst != null) {
                lift.add(icnst);
            }
            HenselApprox<MOD> ab = HenselUtil.<MOD> liftHenselQuadraticFac(C, M, F.get(0), F.get(1));
            lift.add(ab.A);
            lift.add(ab.B);
            return lift;
        }
        BigInteger lc = C.leadingBaseCoefficient();
        GenPolynomial<MOD> f = F.get(0);
        GenPolynomialRing<MOD> mfac = f.ring;
        ModularRingFactory<MOD> mr = (ModularRingFactory<MOD>) mfac.coFac;
        BigInteger P = mr.getIntegerModul();
        // split list in two parts and prepare polynomials
        int k = n / 2;
        int k0 = 0;
        List<GenPolynomial<MOD>> F1 = new ArrayList<GenPolynomial<MOD>>(k);
        GenPolynomial<MOD> A;
        while (true) {
            A = mfac.getONE();
            MOD mlc = mr.fromInteger(lc.getVal());
            A = A.multiply(mlc);
            for (int i = k0; i < k; i++) {
                GenPolynomial<MOD> fi = F.get( i % k ); // take care
                if (fi != null && !fi.isZERO()) {
                    A = A.multiply(fi);
                    F1.add(fi);
                    //} else {
                    //System.out.println("A = " + A + ", fi = " + fi);
                }
            }
            if ( D.get( (int)A.degree(0) ) ) {
                break;
            } else {
                logger.info("skipped by degree set = " + D + ", deg = " + A.degree(0));
                F1.clear();
                k0++;
                k++;
                if ( k0 >= n ) {
                    throw new NoLiftingException("no degree compatible lifting");
                }
            }
        } 
        //System.out.println("A = " + A);
        List<GenPolynomial<MOD>> F0 = new ArrayList<GenPolynomial<MOD>>(F);
        F0.removeAll(F1);
        List<GenPolynomial<MOD>> F2 = new ArrayList<GenPolynomial<MOD>>(k);
        GenPolynomial<MOD> B = mfac.getONE();
        for (GenPolynomial<MOD> fi : F0) {
            if (fi != null && !fi.isZERO()) {
                B = B.multiply(fi);
                F2.add(fi);
                //} else {
                //System.out.println("B = " + B + ", fi = " + fi);
            }
        }
        //System.out.println("B = " + B);
        System.out.println("A = " + A + ", F1 = " + F1);
        System.out.println("B = " + B + ", F2 = " + F2);
        // one Hensel step on part polynomials
        HenselApprox<MOD> ab = HenselUtil.<MOD> liftHenselQuadraticFac(C, M, A, B);
        GenPolynomial<BigInteger> A1 = ab.A;
        GenPolynomial<BigInteger> B1 = ab.B;
        if ( P.equals(A1.multiply(B1)) ) {
            lift.add(A1);
            lift.add(B1);
            logger.info("added exact factors = " + A1 + ", B1 = " + B1);
            //return;
        }
//      if (!isHenselLift(C, M, P, A1, B1)) {
//          throw new NoLiftingException("no lifting A1, B1");
//      }
//      System.out.println("Am = " + ab.Am + ", Bm = " + ab.Bm);
            System.out.println("A1  = " + A1 + ", B1  = " + B1);
            System.out.println("A1m = " + ab.Am + ", B1m = " + ab.Bm);
        BigInteger Mh = M.divide(new BigInteger(2));
        // recursion on list parts
        List<GenPolynomial<BigInteger>> G1 = HenselUtil.<MOD> liftHenselQuadraticFac(A1, Mh, F1, D);
//         if (!isHenselLift(A1, Mh, P, G1)) {
//             throw new NoLiftingException("no lifting G1");
//         }
            System.out.println("A1 = " + A1 + ", F1 = " + F1);
            System.out.println("G1 = " + G1);
            List<GenPolynomial<BigInteger>> G2 = HenselUtil.<MOD> liftHenselQuadraticFac(B1, Mh, F2, D);
//         if (!isHenselLift(B1, Mh, P, G2)) {
//             throw new NoLiftingException("no lifting G2");
//         }
            System.out.println("B1 = " + B1 + ", F2 = " + F2);
            System.out.println("G2 = " + G2);
        if (icnst != null) {
            lift.add(icnst);
        }
        lift.addAll(G1);
        lift.addAll(G2);
        return lift;
    }


    /**
     * Modular Hensel lifting test. Let p be a prime number and assume C ==
     * prod_{0,...,n-1} g_i mod p with ggt(g_i,g_j) == 1 mod p for i != j.
     * @param C GenPolynomial
     * @param G = [g_0,...,g_{n-1}] list of GenPolynomial
     * @param M bound on the coefficients of g_i as factors of C.
     * @param p prime number.
     * @return true if C = prod_{0,...,n-1} g_i mod p^e, else false.
     */
    public static //<MOD extends GcdRingElem<MOD> & Modular> 
      boolean isHenselLift(GenPolynomial<BigInteger> C, BigInteger M, BigInteger p, List<GenPolynomial<BigInteger>> G) {
        if (C == null || C.isZERO()) {
            return false;
        }
        GenPolynomialRing<BigInteger> pfac = C.ring;
        ModIntegerRing pm = new ModIntegerRing(p.getVal(), true);
        GenPolynomialRing<ModInteger> mfac = new GenPolynomialRing<ModInteger>(pm, pfac);

        // check mod p
        GenPolynomial<ModInteger> cl = mfac.getONE();
        GenPolynomial<ModInteger> hlp;
        for (GenPolynomial<BigInteger> hl : G) {
            //System.out.println("hl       = " + hl);
            hlp = PolyUtil.<ModInteger> fromIntegerCoefficients(mfac, hl);
            //System.out.println("hl mod p = " + hlp);
            cl = cl.multiply(hlp);
        }
        GenPolynomial<ModInteger> cp = PolyUtil.<ModInteger> fromIntegerCoefficients(mfac, C);
        if (!cp.equals(cl)) {
            System.out.println("Hensel precondition wrong!");
            System.out.println("cl    = " + cl);
            System.out.println("cp    = " + cp);
            System.out.println("cp-cl = " + cp.subtract(cl));
            System.out.println("M = " + M + ", p = " + p);
            return false;
        }

        // check mod p^e 
        BigInteger mip = p;
        while (mip.compareTo(M) < 0) {
            mip = mip.multiply(mip); // p
        }
        // mip = mip.multiply(p);
        pm = new ModIntegerRing(mip.getVal(), false);
        mfac = new GenPolynomialRing<ModInteger>(pm, pfac);
        cl = mfac.getONE();
        for (GenPolynomial<BigInteger> hl : G) {
            //System.out.println("hl         = " + hl);
            hlp = PolyUtil.<ModInteger> fromIntegerCoefficients(mfac, hl);
            //System.out.println("hl mod p^e = " + hlp);
            cl = cl.multiply(hlp);
        }
        cp = PolyUtil.<ModInteger> fromIntegerCoefficients(mfac, C);
        if (!cp.equals(cl)) {
            System.out.println("Hensel post condition wrong!");
            System.out.println("cl    = " + cl);
            System.out.println("cp    = " + cp);
            System.out.println("cp-cl = " + cp.subtract(cl));
            System.out.println("M = " + M + ", p = " + p + ", p^e = " + mip);
            return false;
        }
        return true;
    }


    /**
     * Modular Hensel lifting test. Let p be a prime number and assume C == A *
     * B mod p with ggt(A,B) == 1 mod p.
     * @param C GenPolynomial
     * @param A GenPolynomial
     * @param B GenPolynomial
     * @param M bound on the coefficients of A and B as factors of C.
     * @param p prime number.
     * @return true if C = A * B mod p**e, else false.
     */
    public static //<MOD extends GcdRingElem<MOD> & Modular>
      boolean isHenselLift(GenPolynomial<BigInteger> C, BigInteger M, BigInteger p, 
                           GenPolynomial<BigInteger> A, GenPolynomial<BigInteger> B) {
        List<GenPolynomial<BigInteger>> G = new ArrayList<GenPolynomial<BigInteger>>(2);
        G.add(A);
        G.add(B);
        return isHenselLift(C, M, p, G);
    }


    /**
     * Modular Hensel lifting test. Let p be a prime number and assume C == A *
     * B mod p with ggt(A,B) == 1 mod p.
     * @param C GenPolynomial
     * @param Ha Hensel approximation.
     * @param M bound on the coefficients of A and B as factors of C.
     * @param p prime number.
     * @return true if C = A * B mod p^e, else false.
     */
    public static <MOD extends GcdRingElem<MOD> & Modular>
      boolean isHenselLift(GenPolynomial<BigInteger> C, BigInteger M, BigInteger p, HenselApprox<MOD> Ha) {
        List<GenPolynomial<BigInteger>> G = new ArrayList<GenPolynomial<BigInteger>>(2);
        G.add(Ha.A);
        G.add(Ha.B);
        return isHenselLift(C, M, p, G);
    }


    /**
     * Modular Hensel lifting algorithm for extended Euclidean relation. Let p =
     * f_i.ring.coFac.modul() i = 0, ..., n-1 and assume C == prod_{0,...,n-1}
     * f_i mod p with ggt(f_i,f_j) == 1 mod p for i != j
     * @param A modular GenPolynomial
     * @param B modular GenPolynomial
     * @param k desired approximation p^k.
     * @return [s,t] with s A + t B = 1 mod p^k.
     */
    public static <MOD extends GcdRingElem<MOD> & Modular>
      GenPolynomial<MOD>[] liftExtendedEuclideanOld(GenPolynomial<MOD> A, GenPolynomial<MOD> B, long k) throws NoLiftingException {
        if (A == null || A.isZERO() || B == null || B.isZERO()) {
            throw new RuntimeException("A and B must be nonzero");
        }
        GenPolynomialRing<MOD> fac = A.ring;
        if (fac.nvar != 1) { // todo assert
            throw new RuntimeException("polynomial ring not univariate");
        }
        // start with extended Euclid mod p
        GenPolynomial<MOD>[] gst = A.egcd(B);
        if (!gst[0].isONE()) {
            throw new NoLiftingException("A and B not coprime, gcd = " + gst[0] + ", A = " + A + ", B = " + B);
        }
        GenPolynomial<MOD> S = gst[1];
        GenPolynomial<MOD> T = gst[2];
        System.out.println("\nS = " + S);
        System.out.println("T = " + T);

        // setup integer polynomial ring
        GenPolynomialRing<BigInteger> ifac = new GenPolynomialRing<BigInteger>(new BigInteger(),fac); 
        GenPolynomial<BigInteger> one = ifac.getONE();
        GenPolynomial<BigInteger> Ai = PolyUtil.integerFromModularCoefficients(ifac, A);
        GenPolynomial<BigInteger> Bi = PolyUtil.integerFromModularCoefficients(ifac, B);
        GenPolynomial<BigInteger> Si = PolyUtil.integerFromModularCoefficients(ifac, S);
        GenPolynomial<BigInteger> Ti = PolyUtil.integerFromModularCoefficients(ifac, T);
        //System.out.println("Ai = " + Ai);
        //System.out.println("Bi = " + Bi);
        //System.out.println("Si = " + Si);
        //System.out.println("Ti = " + Ti);

        // approximate mod p^i
        ModularRingFactory<MOD> mcfac = (ModularRingFactory<MOD>) fac.coFac;
        BigInteger p = mcfac.getIntegerModul();
        BigInteger modul = p;
        GenPolynomialRing<MOD> mfac = new GenPolynomialRing<MOD>(mcfac,fac);
        for ( int i = 0; i < k; i++ ) {
            // e = 1 - s a - t b in Z[x]
            GenPolynomial<BigInteger> e = one.subtract(Si.multiply(Ai)).subtract(Ti.multiply(Bi));
            System.out.println("\ne = " + e);
            e = e.divide(modul);
            // move to in Z_{p^i}[x]
            GenPolynomial<MOD> c = PolyUtil.<MOD> fromIntegerCoefficients(mfac, e);
            System.out.println("c = " + c + ": " + c.ring.coFac);
            GenPolynomial<MOD> s = S.multiply(c);
            GenPolynomial<MOD> t = T.multiply(c);
            //System.out.println("s = " + s + ": " + s.ring.coFac);
            //System.out.println("t = " + t + ": " + t.ring.coFac);

            GenPolynomial<MOD>[] QR = s.divideAndRemainder(B);
            GenPolynomial<MOD> q = QR[0];
            s = QR[1];
            t = t.sum(q.multiply(A));
            //System.out.println("s = " + s + ": " + s.ring.coFac);
            //System.out.println("t = " + t + ": " + t.ring.coFac);

            BigInteger m = modul;
            // setup ring mod p^i
            modul = modul.multiply(p);
            if ( ModLongRing.MAX_LONG.compareTo( modul.getVal() ) > 0 ) {
                mcfac = (ModularRingFactory) new ModLongRing(modul.getVal());
            } else {
                mcfac = (ModularRingFactory) new ModIntegerRing(modul.getVal());
            }
            System.out.println("mcfac = " + mcfac);
            mfac = new GenPolynomialRing<MOD>(mcfac,fac);
            //System.out.println("mfac  = " + mfac);

            MOD mo = mcfac.fromInteger(m.getVal());
            //System.out.println("mo    = " + mo);

            S = PolyUtil.<MOD> fromIntegerCoefficients(mfac, PolyUtil.integerFromModularCoefficients(ifac, S));
            T = PolyUtil.<MOD> fromIntegerCoefficients(mfac, PolyUtil.integerFromModularCoefficients(ifac, T));
            s = PolyUtil.<MOD> fromIntegerCoefficients(mfac, PolyUtil.integerFromModularCoefficients(ifac, s));
            t = PolyUtil.<MOD> fromIntegerCoefficients(mfac, PolyUtil.integerFromModularCoefficients(ifac, t));
            //System.out.println("S = " + S + ": " + S.ring.coFac);
            //System.out.println("T = " + T + ": " + T.ring.coFac);
            //System.out.println("s = " + s + ": " + s.ring.coFac);
            //System.out.println("t = " + t + ": " + t.ring.coFac);
            S = S.sum(s.multiply(mo));
            T = T.sum(t.multiply(mo));
            //System.out.println("S = " + S + ": " + S.ring.coFac);
            //System.out.println("T = " + T + ": " + T.ring.coFac);
            Si = PolyUtil.integerFromModularCoefficients(ifac, S);
            Ti = PolyUtil.integerFromModularCoefficients(ifac, T);
            A = PolyUtil.<MOD> fromIntegerCoefficients(mfac, Ai);
            B = PolyUtil.<MOD> fromIntegerCoefficients(mfac, Bi);
            //System.out.println("Si = " + Si);
            //System.out.println("Ti = " + Ti);
            //System.out.println("A  = " + A + ": " + A.ring.coFac);
            //System.out.println("B  = " + B + ": " + B.ring.coFac);
        }
        GenPolynomial<MOD>[] rel = (GenPolynomial<MOD>[]) new GenPolynomial[2];
        rel[0] = S;
        rel[1] = T;
//         rel[2] = A;
//         rel[3] = B;
        return rel;
    }


    /**
     * Modular Hensel lifting algorithm for extended Euclidean relation. 
     * Let p = * A.ring.coFac.modul() and assume ggt(A,B) == 1 mod p
     * @param A modular GenPolynomial
     * @param B modular GenPolynomial
     * @param k desired approximation p^k.
     * @return [s,t] with s A + t B = 1 mod p^k.
     */
    public static <MOD extends GcdRingElem<MOD> & Modular>
      GenPolynomial<MOD>[] liftExtendedEuclidean(GenPolynomial<MOD> A, GenPolynomial<MOD> B, long k) throws NoLiftingException {
        if (A == null || A.isZERO() || B == null || B.isZERO()) {
            throw new RuntimeException("A and B must be nonzero");
        }
        GenPolynomialRing<MOD> fac = A.ring;
        if (fac.nvar != 1) { // todo assert
            throw new RuntimeException("polynomial ring not univariate");
        }
        // start with extended Euclid mod p
        GenPolynomial<MOD>[] gst = A.egcd(B);
        if (!gst[0].isONE()) {
            throw new NoLiftingException("A and B not coprime, gcd = " + gst[0] + ", A = " + A + ", B = " + B);
        }
        GenPolynomial<MOD> S = gst[1];
        GenPolynomial<MOD> T = gst[2];
        System.out.println("\nS = " + S + ": " + S.ring.coFac);
        System.out.println("T = " + T + ": " + S.ring.coFac);

        // setup integer polynomial ring
        GenPolynomialRing<BigInteger> ifac = new GenPolynomialRing<BigInteger>(new BigInteger(),fac); 
        GenPolynomial<BigInteger> one = ifac.getONE();
        GenPolynomial<BigInteger> Ai = PolyUtil.integerFromModularCoefficients(ifac, A);
        GenPolynomial<BigInteger> Bi = PolyUtil.integerFromModularCoefficients(ifac, B);
        GenPolynomial<BigInteger> Si = PolyUtil.integerFromModularCoefficients(ifac, S);
        GenPolynomial<BigInteger> Ti = PolyUtil.integerFromModularCoefficients(ifac, T);
        //System.out.println("Ai = " + Ai);
        //System.out.println("Bi = " + Bi);
        //System.out.println("Si = " + Si);
        //System.out.println("Ti = " + Ti);

        // approximate mod p^i
        ModularRingFactory<MOD> mcfac = (ModularRingFactory<MOD>) fac.coFac;
        BigInteger p = mcfac.getIntegerModul();
        BigInteger modul = p;
        GenPolynomialRing<MOD> mfac = new GenPolynomialRing<MOD>(mcfac,fac);
        for ( int i = 0; i < k; i++ ) {
            // e = 1 - s a - t b in Z[x]
            GenPolynomial<BigInteger> e = one.subtract(Si.multiply(Ai)).subtract(Ti.multiply(Bi));
            System.out.println("\ne = " + e);
            e = e.divide(modul);
            // move to in Z_p[x]
            GenPolynomial<MOD> c = PolyUtil.<MOD> fromIntegerCoefficients(fac, e);
            System.out.println("c = " + c + ": " + c.ring.coFac);
            GenPolynomial<MOD> s = S.multiply(c);
            GenPolynomial<MOD> t = T.multiply(c);
            //System.out.println("s = " + s + ": " + s.ring.coFac);
            //System.out.println("t = " + t + ": " + t.ring.coFac);

            GenPolynomial<MOD>[] QR = s.divideAndRemainder(B);
            GenPolynomial<MOD> q = QR[0];
            s = QR[1];
            t = t.sum(q.multiply(A));
            System.out.println("s = " + s + ": " + s.ring.coFac);
            System.out.println("t = " + t + ": " + t.ring.coFac);

            GenPolynomial<BigInteger> si = PolyUtil.integerFromModularCoefficients(ifac, s);
            GenPolynomial<BigInteger> ti = PolyUtil.integerFromModularCoefficients(ifac, t);
            //System.out.println("si = " + si);
            //System.out.println("ti = " + si);
            Si = Si.sum(si.multiply(modul));
            Ti = Ti.sum(ti.multiply(modul));
            //System.out.println("Si = " + Si);
            //System.out.println("Ti = " + Ti);

            modul = modul.multiply(p);
            System.out.println("modul = " + modul);
        }
        // setup ring mod p^i
        if ( ModLongRing.MAX_LONG.compareTo( modul.getVal() ) > 0 ) {
            mcfac = (ModularRingFactory) new ModLongRing(modul.getVal());
        } else {
            mcfac = (ModularRingFactory) new ModIntegerRing(modul.getVal());
        }
        System.out.println("mcfac = " + mcfac);
        mfac = new GenPolynomialRing<MOD>(mcfac,fac);
        S = PolyUtil.<MOD> fromIntegerCoefficients(mfac, Si);
        T = PolyUtil.<MOD> fromIntegerCoefficients(mfac, Ti);
        //System.out.println("S = " + S + ": " + S.ring.coFac);
        //System.out.println("T = " + T + ": " + T.ring.coFac);
        GenPolynomial<MOD>[] rel = (GenPolynomial<MOD>[]) new GenPolynomial[2];
        rel[0] = S;
        rel[1] = T;
        return rel;
    }

}
