/*
 * $Id$
 */

package edu.jas.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
//import java.util.ListIterator;
import java.util.Map;

import java.io.Serializable;

import org.apache.log4j.Logger;

import edu.jas.structure.RingElem;

//import edu.jas.arith.BigRational;

import edu.jas.poly.GenPolynomial;
import edu.jas.poly.GenPolynomialRing;
import edu.jas.poly.PolynomialList;
import edu.jas.poly.ExpVector;
//import edu.jas.poly.TermOrder;

import edu.jas.ring.Reduction;
import edu.jas.ring.ReductionSeq;
//import edu.jas.ring.GroebnerBase;
import edu.jas.ring.GroebnerBaseSeq;
import edu.jas.ring.GroebnerBaseSeqPairSeq;
import edu.jas.ring.ExtendedGB;

import edu.jas.module.ModuleList;
import edu.jas.module.GenVector;
import edu.jas.module.GenVectorModul;


/**
 * Syzygy class.
 * Implements Syzygy computations and tests.
 * @author Heinz Kredel
 */

public class Syzygy<C extends RingElem<C>>  {

    private static final Logger logger = Logger.getLogger(Syzygy.class);
    private final boolean debug = logger.isDebugEnabled();


    /**
     * Reduction engine.
     */
    protected Reduction<C> red;


    /**
     * Groebner base engine.
     */
    // no    protected GroebnerBase<C> gb;


    /**
     * Constructor.
     */
    public Syzygy() {
        red = new ReductionSeq<C>();
        //gb = new GroebnerBaseSeqPairSeq<C>();
    }


    /**
     * Syzygy module from Groebner base.
     * F must be a Groebner base.
     * @param C coefficient type.
     * @param F a Groebner base.
     * @return syz(F), a basis for the module of syzygies for F.
     */
    public List<List<GenPolynomial<C>>> 
           zeroRelations(List<GenPolynomial<C>> F) {  
        return zeroRelations(0,F);
    }


    /**
     * Syzygy module from Groebner base.
     * F must be a Groebner base.
     * @param C coefficient type.
     * @param modv number of module variables.
     * @param F a Groebner base.
     * @return syz(F), a basis for the module of syzygies for F.
     */
    public List<List<GenPolynomial<C>>> 
           zeroRelations(int modv, List<GenPolynomial<C>> F) {  
        List<List<GenPolynomial<C>>> Z 
           = new ArrayList<List<GenPolynomial<C>>>();
        if ( F == null ) {
           return Z;
        }
        GenVectorModul<GenPolynomial<C>> mfac = null;
        int i = 0;
        while ( mfac == null && i < F.size() ) {
            GenPolynomial<C> p = F.get(i);
            if ( p != null ) {
               mfac = new GenVectorModul<GenPolynomial<C>>( p.ring, 
                                                            F.size() );
            }
        }
        if ( mfac == null ) {
           return Z;
        }
        GenVector<GenPolynomial<C>> v = mfac.fromList( F );
        //System.out.println("F = " + F + " v = " + v);
        return zeroRelations(modv,v);
    }


    /**
     * Syzygy module from Groebner base.
     * v must be a Groebner base.
     * @param C coefficient type.
     * @param modv number of module variables.
     * @param v a Groebner base.
     * @return syz(v), a basis for the module of syzygies for v.
     */
    public List<List<GenPolynomial<C>>> 
           zeroRelations(int modv, GenVector<GenPolynomial<C>> v) {  

        List<List<GenPolynomial<C>>> Z 
           = new ArrayList<List<GenPolynomial<C>>>();

        GenVectorModul<GenPolynomial<C>> mfac = v.modul;
        List<GenPolynomial<C>> F = v.val; 
        GenVector<GenPolynomial<C>> S = mfac.getZERO();
        GenPolynomial<C> pi, pj, s, h, zero;
        zero = mfac.coFac.getZERO();
	for ( int i = 0; i < F.size(); i++ ) {
	    pi = F.get(i);
            for ( int j = i+1; j < F.size(); j++ ) {
                pj = F.get(j);
                //logger.info("p"+i+", p"+j+" = " + pi + ", " +pj);

		if ( ! red.moduleCriterion( modv, pi, pj ) ) {
                   continue;
                }
		// if ( ! red.criterion4( pi, pj ) ) { continue; }
                List<GenPolynomial<C>> row = S.clone().val;

		s = red.SPolynomial( row, i, pi, j, pj );
		if ( s.isZERO() ) {
                   Z.add( row );
                   continue;
                }

		h = red.normalform( row, F, s );
		if ( ! h.isZERO() ) {
                   throw new RuntimeException("Syzygy no GB");
                }
                if ( logger.isDebugEnabled() ) {
                   logger.info("row = " + row.size());
                }
                Z.add( row );
	    }
	}
        return Z;
    }


    /**
     * Syzygy module from module Groebner base.
     * M must be a module Groebner base.
     * @param C coefficient type.
     * @param M a module Groebner base.
     * @return syz(M), a basis for the module of syzygies for M.
     */
    public ModuleList<C> 
           zeroRelations(ModuleList<C> M) {  
        ModuleList<C> N = M;
        if ( M == null || M.list == null) {
            return N;
        }
        if ( M.rows == 0 || M.cols == 0 ) {
            return N;
        }
        GenPolynomial<C> zero = M.ring.getZERO();
        //System.out.println("zero = " + zero);

        //ModuleList<C> Np = null;
        PolynomialList<C> F = M.getPolynomialList();
        int modv = M.cols; // > 0  
        //System.out.println("modv = " + modv);
        List<List<GenPolynomial<C>>> G = zeroRelations(modv,F.list);
        if ( G == null ) {
            return N;
        }
        List<List<GenPolynomial<C>>> Z 
           = new ArrayList<List<GenPolynomial<C>>>();
        for ( int i = 0; i < G.size(); i++ ) {
            //F = new PolynomialList(F.ring,(List)G.get(i));
            List<GenPolynomial<C>> Gi = G.get(i);
            List<GenPolynomial<C>> Zi = new ArrayList<GenPolynomial<C>>();
            // System.out.println("\nG("+i+") = " + G.get(i));
            for ( int j = 0; j < Gi.size(); j++ ) {
                //System.out.println("\nG("+i+","+j+") = " + Gi.get(j));
                GenPolynomial<C> p = Gi.get(j);
                if ( p != null ) {
                   Map<ExpVector,GenPolynomial<C>> r = p.contract( M.ring );
                   int s = 0;
                   for ( GenPolynomial<C> vi : r.values() ) {
                       Zi.add(vi); 
                       s++;
                   }
                   if ( s == 0 ) {
                       Zi.add(zero); 
                   } else if ( s > 1 ) { // will not happen
                       System.out.println("p = " + p );
                       System.out.println("map("+i+","+j+") = " 
                                          + r + ", size = " + r.size() );
                       throw new RuntimeException("Map.size() > 1 = " + r.size());
                   }
                }
            }
            //System.out.println("\nZ("+i+") = " + Zi);
            Z.add( Zi );
        }
        N = new ModuleList<C>(M.ring,Z);
        //System.out.println("\n\nN = " + N);
        return N;
    }


    /**
     * Test if sysygy.
     * @param C coefficient type.
     * @param Z list of sysygies.
     * @param F a polynomial list.
     * @return true, if Z is a list of syzygies for F, else false.
     */

    public boolean 
           isZeroRelation(List<List<GenPolynomial<C>>> Z, 
                          List<GenPolynomial<C>> F) {  
        for ( List<GenPolynomial<C>> row: Z ) {
            GenPolynomial<C> p = scalarProduct(row,F);
            if ( p == null ) { 
               continue;
            }
            if ( ! p.isZERO() ) {
                logger.info("is not ZeroRelation = " + p.toString(p.ring.getVars()));
                logger.info("row = " + row);
                //logger.info("F = " + F);
                return false;
            }
        }
        return true;
    }


    /**
     * Scalar product of vectors of polynomials.
     * @param C coefficient type.
     * @param r a polynomial list.
     * @param F a polynomial list.
     * @return the scalar product of r and F.
     */

    public GenPolynomial<C> 
           scalarProduct(List<GenPolynomial<C>> r, 
                         List<GenPolynomial<C>> F) {  
        GenPolynomial<C> sp = null;
        Iterator<GenPolynomial<C>> it = r.iterator();
        Iterator<GenPolynomial<C>> jt = F.iterator();
        while ( it.hasNext() && jt.hasNext() ) {
            GenPolynomial<C> pi = it.next();
            GenPolynomial<C> pj = jt.next();
            if ( pi == null || pj == null ) {
               continue;
            }
            if ( sp == null ) {
                sp = pi.multiply(pj);
            } else {
                sp = sp.add( pi.multiply(pj) );
            }
        }
        if ( it.hasNext() || jt.hasNext() ) {
            logger.error("scalarProduct wrong sizes");
        }
        return sp;
    }


    /**
     * Test if sysygy of modules.
     * @param C coefficient type.
     * @param Z list of sysygies.
     * @param F a module list.
     * @return true, if Z is a list of syzygies for F, else false.
     */

    public boolean 
           isZeroRelation(ModuleList<C> Z, ModuleList<C> F) {  
        if ( Z == null || Z.list == null ) {
            return true;
        }
        for ( List<GenPolynomial<C>> row : Z.list ) {
            List<GenPolynomial<C>> zr = scalarProduct(row,F);
            if ( ! isZero(zr) ) {
                logger.info("is not ZeroRelation (" + zr.size() + ") = " + zr);
                return false;
            }
        }
        return true;
    }


    /**
     * product of vector and matrix of polynomials.
     * @param C coefficient type.
     * @param r a polynomial list.
     * @param F a polynomial matrix.
     * @return the scalar product of r and F.
     */

    public List<GenPolynomial<C>> 
           scalarProduct(List<GenPolynomial<C>> r, ModuleList<C> F) {  
        List<GenPolynomial<C>> ZZ = null;
        Iterator<GenPolynomial<C>> it = r.iterator();
        Iterator<List<GenPolynomial<C>>> jt = F.list.iterator();
        while ( it.hasNext() && jt.hasNext() ) {
            GenPolynomial<C> pi = it.next();
            List<GenPolynomial<C>> vj = jt.next();
            List<GenPolynomial<C>> Z = scalarProduct( pi, vj );
            //System.out.println("pi" + pi);
            //System.out.println("vj" + vj);
            // System.out.println("scalarProduct" + Z);
            if ( ZZ == null ) {
                ZZ = Z;
            } else {
                ZZ = vectorAdd(ZZ,Z);
            }
        }
        if ( it.hasNext() || jt.hasNext() ) {
            logger.error("scalarProduct wrong sizes");
        }
        if ( logger.isDebugEnabled() ) {
            logger.debug("scalarProduct" + ZZ);
        }
        return ZZ;
    }


    /**
     * Addition of vectors of polynomials.
     * @param C coefficient type.
     * @param a a polynomial list.
     * @param b a polynomial list.
     * @return a+b, the vector sum of a and b.
     */

    public List<GenPolynomial<C>> 
           vectorAdd(List<GenPolynomial<C>> a, List<GenPolynomial<C>> b) {  
        if ( a == null ) {
            return b;
        }
        if ( b == null ) {
            return a;
        }
        List<GenPolynomial<C>> V = new ArrayList<GenPolynomial<C>>( a.size() );
        Iterator<GenPolynomial<C>> it = a.iterator();
        Iterator<GenPolynomial<C>> jt = b.iterator();
        while ( it.hasNext() && jt.hasNext() ) {
            GenPolynomial<C> pi = it.next();
            GenPolynomial<C> pj = jt.next();
            GenPolynomial<C> p = pi.add( pj );
            V.add( p );
        }
        //System.out.println("vectorAdd" + V);
        if ( it.hasNext() || jt.hasNext() ) {
            logger.error("vectorAdd wrong sizes");
        }
        return V;
    }


    /**
     * test vector of zero polynomials.
     * @param C coefficient type.
     * @param a a polynomial list.
     * @return true, if all polynomial in a are zero, else false.
     */
    public boolean 
           isZero(List<GenPolynomial<C>> a) {  
        if ( a == null ) {
            return true;
        }
        for ( GenPolynomial<C> pi : a ) {
            if ( pi == null ) {
                continue;
            }
            if ( ! pi.isZERO() ) {
                return false;
            }
        }
        return true;
    }


    /**
     * Scalar product of polynomial with vector of polynomials.
     * @param C coefficient type.
     * @param p a polynomial.
     * @param F a polynomial list.
     * @return the scalar product of p and F.
     */

    public List<GenPolynomial<C>> 
           scalarProduct(GenPolynomial<C> p, List<GenPolynomial<C>> F) {  
        List<GenPolynomial<C>> V = new ArrayList<GenPolynomial<C>>( F.size() );
        for ( GenPolynomial<C> pi : F ) {
            if ( p != null ) {
               pi = p.multiply( pi );
            } else {
               pi = null;
            }
            V.add( pi );
        }
        return V;
    }


    /**
     * Scalar product of vector of polynomials with polynomial.
     * @param C coefficient type.
     * @param F a polynomial list.
     * @param p a polynomial.
     * @return the scalar product of F and p.
     */

    public List<GenPolynomial<C>> 
        scalarProduct(List<GenPolynomial<C>> F, GenPolynomial<C> p) {  
        List<GenPolynomial<C>> V = new ArrayList<GenPolynomial<C>>( F.size() );
        for ( GenPolynomial<C> pi : F ) {
            if ( pi != null ) {
               pi = pi.multiply( p );
            }
            V.add( pi );
        }
        return V;
    }


    /**
     * Resolution of a module.
     * Only with direct GBs.
     * @param C coefficient type.
     * @param M a module list of a Groebner basis.
     * @return a resolution of M.
     */
    public List<ResPart<C>>
           resolution(ModuleList<C> M) {  
        List<ResPart<C>> R = new ArrayList<ResPart<C>>();
        ModuleList<C> MM = M;
        ModuleList<C> GM;
        ModuleList<C> Z;
        ModGroebnerBase<C> mbb = new ModGroebnerBase<C>();
        while (true) {
          GM = mbb.GB(MM);
          Z = zeroRelations(GM);
          R.add( new ResPart<C>(MM,GM,Z) );
          if ( Z == null || Z.list == null || Z.list.size() == 0 ) {
              break;
          }
          MM = Z;
        }
        return R;
    }


    /**
     * Resolution of a polynomial list.
     * Only with direct GBs.
     * @param C coefficient type.
     * @param F a polynomial list of a Groebner basis.
     * @return a resolution of F.
     */
    public List // <ResPart<C>|ResPolPart<C>>
           resolution(PolynomialList<C> F) {  
        List<List<GenPolynomial<C>>> Z;
        ModuleList<C> Zm;
        List<GenPolynomial<C>> G;
        PolynomialList<C> Gl;

        G = (new GroebnerBaseSeq<C>()).GB( F.list );
        Z = zeroRelations( G );
        Gl = new PolynomialList<C>(F.ring, G);
        Zm = new ModuleList<C>(F.ring, Z);

        List R = resolution(Zm); //// <ResPart<C>|ResPolPart<C>>
        R.add( 0, new ResPolPart<C>( F, Gl, Zm ) ); 
        return R;
    }


    /**
     * Syzygy module from arbitrary base.
     * @param C coefficient type.
     * @param F a polynomial list.
     * @return syz(F), a basis for the module of syzygies for F.
     */
    public List<List<GenPolynomial<C>>> 
           zeroRelationsArbitrary(List<GenPolynomial<C>> F) {  
        return zeroRelationsArbitrary(0,F);
    }


    /**
     * Syzygy module from arbitrary base.
     * @param C coefficient type.
     * @param modv number of module variables.
     * @param F a polynomial list.
     * @return syz(F), a basis for the module of syzygies for F.
     */
    public List<List<GenPolynomial<C>>> 
        zeroRelationsArbitrary(int modv, List<GenPolynomial<C>> F) {  

        if ( F == null ) {
            return zeroRelations( modv, F );
        }
        if ( F.size() <= 1 ) {
            return zeroRelations( modv, F );
        }
        final int lenf = F.size(); 
        GroebnerBaseSeqPairSeq<C> gb = new GroebnerBaseSeqPairSeq<C>();
        ExtendedGB<C> exgb = gb.extGB( F );
        if ( debug ) {
           logger.debug("exgb = " + exgb);
        }
        if ( ! gb.isReductionMatrix(exgb) ) {
           logger.error("is reduction matrix ? false");
        }

        List<GenPolynomial<C>> G = exgb.G;
        List<List<GenPolynomial<C>>> G2F = exgb.G2F;
        List<List<GenPolynomial<C>>> F2G = exgb.F2G;

        List<List<GenPolynomial<C>>> sg = zeroRelations( modv, G );
        GenPolynomialRing<C> ring = G.get(0).ring;
        ModuleList<C> S = new ModuleList<C>( ring, sg );
        if ( debug ) {
           logger.debug("syz = " + S);
        }
        if ( ! isZeroRelation(sg,G) ) {
           logger.error("is syzygy ? false");
        }

        List<List<GenPolynomial<C>>> sf;
        sf = new ArrayList<List<GenPolynomial<C>>>( sg.size() );
        //List<GenPolynomial<C>> row;

        for ( List<GenPolynomial<C>> r : sg ) {
            Iterator<GenPolynomial<C>> it = r.iterator();
            Iterator<List<GenPolynomial<C>>> jt = G2F.iterator();

            List<GenPolynomial<C>> rf;
            rf = new ArrayList<GenPolynomial<C>>( lenf );
            for ( int m = 0; m < lenf; m++ ) {
                rf.add( ring.getZERO() );
            }
            while ( it.hasNext() && jt.hasNext() ) {
               GenPolynomial<C> si = it.next();
               List<GenPolynomial<C>> ai = jt.next();
               //System.out.println("si = " + si);
               //System.out.println("ai = " + ai);
               if ( si == null || ai == null ) {
                  continue;
               }
               List<GenPolynomial<C>> pi = scalarProduct(si,ai);
               //System.out.println("pi = " + pi);
               rf = vectorAdd( rf, pi );
            }
            if ( it.hasNext() || jt.hasNext() ) {
               logger.error("zeroRelationsArbitrary wrong sizes");
            }
            //System.out.println("\nrf = " + rf + "\n");
            sf.add( rf );
        }


        List<List<GenPolynomial<C>>> M;
        M = new ArrayList<List<GenPolynomial<C>>>( lenf );
        for ( List<GenPolynomial<C>> r : F2G ) {
            Iterator<GenPolynomial<C>> it = r.iterator();
            Iterator<List<GenPolynomial<C>>> jt = G2F.iterator();

            List<GenPolynomial<C>> rf;
            rf = new ArrayList<GenPolynomial<C>>( lenf );
            for ( int m = 0; m < lenf; m++ ) {
                rf.add( ring.getZERO() );
            }
            while ( it.hasNext() && jt.hasNext() ) {
               GenPolynomial<C> si = it.next();
               List<GenPolynomial<C>> ai = jt.next();
               //System.out.println("si = " + si);
               //System.out.println("ai = " + ai);
               if ( si == null || ai == null ) {
                  continue;
               }
               List<GenPolynomial<C>> pi = scalarProduct(ai,si);
               //System.out.println("pi = " + pi);
               rf = vectorAdd( rf, pi );
            }
            if ( it.hasNext() || jt.hasNext() ) {
               logger.error("zeroRelationsArbitrary wrong sizes");
            }
            //System.out.println("\nMg Mf = " + rf + "\n");
            M.add( rf );
        }
        //ModuleList<C> ML = new ModuleList<C>( ring, M );
        //System.out.println("syz ML = " + ML);
        // debug only:
        List<GenPolynomial<C>> F2 = new ArrayList<GenPolynomial<C>>( F.size() );
        for ( List<GenPolynomial<C>> rr: M ) {
            GenPolynomial<C> rrg = scalarProduct( F, rr );
            F2.add( rrg );
        }
        PolynomialList<C> pF = new PolynomialList<C>( ring, F );
        PolynomialList<C> pF2 = new PolynomialList<C>( ring, F2 );
        if ( ! pF.equals( pF2 ) ) {
           logger.error("is FAB = F ? false");
        }

        int sflen = sf.size();
        List<List<GenPolynomial<C>>> M2;
        M2 = new ArrayList<List<GenPolynomial<C>>>( lenf );
        int i = 0;
        for ( List<GenPolynomial<C>> ri: M ) {
            List<GenPolynomial<C>> r2i;
            r2i = new ArrayList<GenPolynomial<C>>( ri.size() );
            int j = 0;
            for ( GenPolynomial<C> rij: ri ) {
                GenPolynomial<C> p = null;
                if ( i == j ) {
                    p = ring.getONE().subtract( rij );
                } else {
                    if ( rij != null ) {
                       p = rij.negate();
                    }
                }
                r2i.add( p );
                j++;
            }
            M2.add( r2i );
            if ( ! isZero( r2i ) ) {
                sf.add( r2i );
            }
            i++;
        }
        ModuleList<C> M2L = new ModuleList<C>( ring, M2 );
        if ( debug ) {
           logger.debug("syz M2L = " + M2L);
        }

        if ( debug ) {
           ModuleList<C> SF = new ModuleList<C>( ring, sf );
           logger.debug("syz sf = " + SF);
           logger.debug("#syz " + sflen + ", " + sf.size());
        }
        if ( ! isZeroRelation(sf,F) ) {
           logger.error("is syz sf ? false");
        }
        return sf;
    }

}


/**
 * Container for module resolution components.
 */
class ResPart<C extends RingElem<C>> implements Serializable {

    public final ModuleList<C> module;
    public final ModuleList<C> GB;
    public final ModuleList<C> syzygy;

   /**
     * ResPart.
     * @param m a module list.
     * @param g a module list GB.
     * @param z a syzygy module list.
     */
    public ResPart(ModuleList<C> m, ModuleList<C> g, ModuleList<C> z) {
        module = m;
        GB = g;
        syzygy = z;
    }


/**
 * toString.
 */
    public String toString() {
        StringBuffer s = new StringBuffer("ResPart(\n");
        s.append("module = " + module);
        s.append("\n GB = " + GB);
        s.append("\n syzygy = " + syzygy);
        s.append(")");
        return s.toString();
    }
}


/**
 * Container for polynomial resolution components.
 */
class ResPolPart<C extends RingElem<C>> implements Serializable {

    public final PolynomialList<C> ideal;
    public final PolynomialList<C> GB;
    public final ModuleList<C> syzygy;

    /**
      * ResPolPart.
      * @param m a polynomial list.
      * @param g a polynomial list GB.
      * @param z a syzygy module list.
      */
    public ResPolPart(PolynomialList<C> m, PolynomialList<C> g, ModuleList<C> z) {
        ideal = m;
        GB = g;
        syzygy = z;
    }


   /**
     * toString.
     */
    public String toString() {
        StringBuffer s = new StringBuffer("ResPolPart(\n");
        s.append("ideal = " + ideal);
        s.append("\n GB = " + GB);
        s.append("\n syzygy = " + syzygy);
        s.append(")");
        return s.toString();
    }

}
