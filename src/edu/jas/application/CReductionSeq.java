/*
 * $Id$
 */

package edu.jas.application;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.jas.poly.ExpVector;
import edu.jas.poly.GenPolynomial;
import edu.jas.poly.GenPolynomialRing;
import edu.jas.poly.ColorPolynomial;

import edu.jas.structure.RingElem;
import edu.jas.structure.RingFactory;
import edu.jas.structure.GcdRingElem;

import edu.jas.application.Ideal;


/**
 * Polynomial parametric ring Reduction sequential use algorithm.
 * Implements normalform and coloring and condition stuff.
 * @param <C> coefficient type
 * @author Heinz Kredel
 */

public class CReductionSeq<C extends GcdRingElem<C>>
             /*extends ReductionAbstract<C>*/ 
             /*implements CReduction<C>*/ {

    private static final Logger logger = Logger.getLogger(CReductionSeq.class);
    private final boolean debug = logger.isDebugEnabled();


    /**
     * Constructor.
     */
    public CReductionSeq() {
    }


    /**
     * S-Polynomial.
     * @param Ap polynomial.
     * @param Bp polynomial.
     * @return spol(Ap,Bp) the S-polynomial of Ap and Bp.
     */
    public ColorPolynomial<C> 
           SPolynomial(ColorPolynomial<C> Ap, 
                       ColorPolynomial<C> Bp) {  
        if ( Bp == null || Bp.isZERO() ) {
           return Bp;
        }
        if ( Ap == null || Ap.isZERO() ) {
            return Ap;
        }

        Map.Entry<ExpVector,GenPolynomial<C>> ma = Ap.red.leadingMonomial();
        Map.Entry<ExpVector,GenPolynomial<C>> mb = Bp.red.leadingMonomial();

        ExpVector e = ma.getKey();
        ExpVector f = mb.getKey();

        ExpVector g  = ExpVector.EVLCM(e,f);
        ExpVector e1 = ExpVector.EVDIF(g,e);
        ExpVector f1 = ExpVector.EVDIF(g,f);

        GenPolynomial<C> a = ma.getValue();
        GenPolynomial<C> b = mb.getValue();

        ColorPolynomial<C> App = Ap.multiply( b, e1 );
        ColorPolynomial<C> Bpp = Bp.multiply( a, f1 );
        ColorPolynomial<C> Cp = App.subtract(Bpp);
        return Cp;
    }


    /**
     * S-Polynomial with recording.
     * @param S recording matrix, is modified. 
     *        <b>Note</b> the negative Spolynomial is recorded as 
     *        required by all applications.
     * @param i index of Ap in basis list.
     * @param Ap a polynomial.
     * @param j index of Bp in basis list.
     * @param Bp a polynomial.
     * @return Spol(Ap, Bp), the S-Polynomial for Ap and Bp.
     */
    public GenPolynomial<C> 
        SPolynomial(List<GenPolynomial<C>> S,
                    int i,
                    GenPolynomial<C> Ap, 
                    int j,
                    GenPolynomial<C> Bp) {  
        throw new RuntimeException("not implemented");
    }


    /**
     * Is top reducible.
     * @param A polynomial.
     * @param P polynomial list.
     * @return true if A is top reducible with respect to P.
     */
    public boolean isTopReducible(List<ColorPolynomial<C>> P, 
                                  ColorPolynomial<C> A) {  
        if ( P == null || P.isEmpty() ) {
           return false;
        }
        if ( A == null || A.isZERO() ) {
           return false;
        }
        boolean mt = false;
        ExpVector e = A.leadingExpVector();
        for ( ColorPolynomial<C> p : P ) {
            mt = ExpVector.EVMT( e, p.leadingExpVector() );
            if ( mt ) {
               return true;
            } 
        }
        return false;
    }


    /**
     * Is reducible.
     * @param Ap polynomial.
     * @param Pp polynomial list.
     * @return true if Ap is reducible with respect to Pp.
     */
    public boolean isReducible(List<ColorPolynomial<C>> Pp, 
                               ColorPolynomial<C> Ap) {  
        return !isNormalform(Pp,Ap);
    }


    /**
     * Is in Normalform.
     * @param Ap polynomial.
     * @param Pp polynomial list.
     * @return true if Ap is in normalform with respect to Pp.
     */
    @SuppressWarnings("unchecked") 
    public boolean isNormalform(List<ColorPolynomial<C>> Pp, 
                                ColorPolynomial<C> Ap) {  
        if ( Pp == null || Pp.isEmpty() ) {
           return true;
        }
        if ( Ap == null || Ap.isZERO() ) {
           return true;
        }
        int l;
        ColorPolynomial<C>[] P;
        synchronized (Pp) {
            l = Pp.size();
            P = new ColorPolynomial[l];
            //P = Pp.toArray();
            for ( int i = 0; i < Pp.size(); i++ ) {
                P[i] = Pp.get(i);
            }
        }
        ExpVector[] htl = new ExpVector[ l ];
        ColorPolynomial<C>[] p = new ColorPolynomial[ l ];
        Map.Entry<ExpVector,GenPolynomial<C>> m;
        int i;
        int j = 0;
        for ( i = 0; i < l; i++ ) { 
            p[i] = P[i];
            m = p[i].red.leadingMonomial();
            if ( m != null ) { 
               p[j] = p[i];
               htl[j] = m.getKey();
               j++;
            }
        }
        l = j;
        boolean mt = false;
        for ( ExpVector e : Ap.red.getMap().keySet() ) { 
            for ( i = 0; i < l; i++ ) {
                mt = ExpVector.EVMT( e, htl[i] );
                if ( mt ) {
                   return false;
                } 
            }
        }
        for ( ExpVector e : Ap.white.getMap().keySet() ) { 
            for ( i = 0; i < l; i++ ) {
                mt = ExpVector.EVMT( e, htl[i] );
                if ( mt ) {
                   return false;
                } 
            }
        }
        return true;
    }


    /**
     * Is in Normalform.
     * @param Pp polynomial list.
     * @return true if each Ap in Pp is in normalform with respect to Pp\{Ap}.
     */
    public boolean isNormalform( List<ColorPolynomial<C>> Pp ) {  
        if ( Pp == null || Pp.isEmpty() ) {
           return true;
        }
        ColorPolynomial<C> Ap;
        List<ColorPolynomial<C>> P = new LinkedList<ColorPolynomial<C>>( Pp );
        int s = P.size();
        for ( int i = 0; i < s; i++ ) {
            Ap = P.remove(i);
            if ( ! isNormalform(P,Ap) ) {
               return false;
            }
            P.add(Ap);
        }
        return true;
    }


    /**
     * Normalform.
     * @param Ap polynomial.
     * @param Pp polynomial list.
     * @return nf(Ap) with respect to Pp.
     */
    @SuppressWarnings("unchecked") 
    public ColorPolynomial<C> normalform(List<ColorPolynomial<C>> Pp, 
                                         ColorPolynomial<C> Ap) {  
        if ( Pp == null || Pp.isEmpty() ) {
           return Ap;
        }
        if ( Ap == null || Ap.isZERO() ) {
           return Ap;
        }
        Map.Entry<ExpVector,GenPolynomial<C>> m;
        int l;
        ColorPolynomial<C>[] P;
        synchronized (Pp) {
            l = Pp.size();
            P = new ColorPolynomial[l];
            //P = Pp.toArray();
            for ( int i = 0; i < Pp.size(); i++ ) {
                P[i] = Pp.get(i);
            }
        }
        ExpVector[] htl = new ExpVector[ l ];
        Object[] lbc = new Object[ l ]; // want C[] 
        ColorPolynomial<C>[] p = new ColorPolynomial[ l ];
        int i;
        int j = 0;
        for ( i = 0; i < l; i++ ) { 
            if ( P[i] == null ) {
               continue;
            }
            p[i] = P[i];
            m = p[i].red.leadingMonomial();
            if ( m != null ) { 
               p[j] = p[i];
               htl[j] = m.getKey();
               lbc[j] = m.getValue();
               j++;
            }
        }
        l = j;
        ExpVector e;
        GenPolynomial<C> a;
        boolean mt = false;
        GenPolynomial<GenPolynomial<C>> zero = p[0].red.ring.getZERO();
        ColorPolynomial<C> R = new ColorPolynomial<C>(zero,zero,zero);

        //ColorPolynomial<C> T = null;
        ColorPolynomial<C> Q = null;
        ColorPolynomial<C> S = Ap;
        while ( S.length() > 0 ) { 
              m = S.leadingMonomial();
              e = m.getKey();
              a = m.getValue();
              //System.out.println("NF, e = " + e);
              for ( i = 0; i < l; i++ ) {
                  mt = ExpVector.EVMT( e, htl[i] );
                  if ( mt ) break; 
              }
              if ( ! mt ) { 
                 //logger.debug("irred");
                 //T = new OrderedMapPolynomial( a, e );
                 R = R.sum( a, e );
                 S = S.subtract( a, e ); 
                 // System.out.println(" S = " + S);
              } else { 
                 e = ExpVector.EVDIF( e, htl[i] );
                 //logger.info("red div = " + e);
                 GenPolynomial<C> c = (GenPolynomial<C>) lbc[i];
                 if ( a.remainder(c).isZERO() ) {   //c.isUnit() ) {
                    a = a.divide( c );
                 } else {
                    S = S.multiply( c );
                    R = R.multiply( c );
                 }
                 Q = p[i].multiply( a, e );
                 S = S.subtract( Q );
              }
        }
        return R;
    }


    /**
     * Normalform with recording.
     * @param row recording matrix, is modified.
     * @param Pp a polynomial list for reduction.
     * @param Ap a polynomial.
     * @return nf(Pp,Ap), the normal form of Ap wrt. Pp.
     */
    @SuppressWarnings("unchecked") 
        public ColorPolynomial<C> 
        normalform(List<ColorPolynomial<C>> row,
                   List<ColorPolynomial<C>> Pp, 
                   ColorPolynomial<C> Ap) {  
        if ( Pp == null || Pp.isEmpty() ) {
            return Ap;
        }
        if ( Ap == null || Ap.isZERO() ) {
            return Ap;
        }
        throw new RuntimeException("not implemented");
        //return Ap;
    }


    /*
     * -------- coloring and condition stuff ------------------------------
     */

    /**
     * Determine polynomial relative to conditions in F.
     * @param A polynomial.
     * @return new F.
     */
    public List<ColoredSystem<C>> 
        determine( List<ColoredSystem<C>> CS, 
                   GenPolynomial<GenPolynomial<C>> A) {  
        if ( A == null || A.isZERO() ) {
            return CS;
        }
        GenPolynomial<GenPolynomial<C>> zero = A.ring.getZERO();
        List<ColoredSystem<C>> NCS = new ArrayList<ColoredSystem<C>>( CS ); //.size() );
        for ( ColoredSystem<C> cs : CS ) {
            GenPolynomial<GenPolynomial<C>> green = zero;
            GenPolynomial<GenPolynomial<C>> red;
            GenPolynomial<GenPolynomial<C>> white;
            GenPolynomial<GenPolynomial<C>> Ap = A;
            GenPolynomial<GenPolynomial<C>> Bp;
            ColorPolynomial<C> nz;
            ColoredSystem<C> NS;
            Ideal<C> id = cs.conditions; //.clone();
            //System.out.println("starting id = " + id);
            List<ColorPolynomial<C>> S = cs.list;
            List<ColorPolynomial<C>> Sp;
            while( !Ap.isZERO() ) {
                Map.Entry<ExpVector,GenPolynomial<C>> m = Ap.leadingMonomial();
                ExpVector e = m.getKey();
                GenPolynomial<C> c = m.getValue();
                Bp = Ap.reductum();
                if ( c.isConstant() ) {
                   red = zero.sum(c,e);
                   white = Bp;
                   nz = new ColorPolynomial<C>(green,red,white); 
                   //System.out.println("nz = " + nz);
                   Sp = new ArrayList<ColorPolynomial<C>>( S );
                   Sp.add( nz );
                   NS = new ColoredSystem<C>( id, Sp );
                   NS = NS.reDetermine();
                   //System.out.println("NS = " + NS);
                   NCS.add( NS );
                   break;
                }
                if ( id.contains(c) ) {
                   //System.out.println("c in id = " + c);
                   green = green.sum(c,e);
                   Ap = Bp;
                   continue;
                }
                red = zero.sum(c,e);
                white = Bp;
                nz = new ColorPolynomial<C>(green,red,white); 
                //System.out.println("nz = " + nz);
                Sp = new ArrayList<ColorPolynomial<C>>( S );
                Sp.add( nz );
                // re determine existing polynomials
                NS = new ColoredSystem<C>( id, Sp );
                if ( logger.isDebugEnabled() ) {
                   logger.info("new determined " + NS);
                }
                NS = NS.reDetermine();
                if ( logger.isDebugEnabled() ) {
                   logger.info("re determined " + NS);
                }
                //System.out.println("NS = " + NS);
                NCS.add( NS );

                id = id.sum( c );
                //System.out.println("id = " + id);
                if ( id.isONE() ) { // can treat remaining coeffs as green
                   //System.out.println("dropping " + cs);
                   //System.out.println("by green " + green.sum(c,e));
                   break; // drop system
                }
                green = green.sum(c,e);
                Ap = Bp;
                if ( Bp.isZERO() ) {
                    // add green poly and condition
                    red = red.subtract(c,e);
                    nz = new ColorPolynomial<C>(green,red,white); 
                    System.out.println("nz = " + nz);
                    //if ( !nz.isZERO() ) {
                        Sp = new ArrayList<ColorPolynomial<C>>( S );
                        Sp.add( nz );
                        // re determine existing polynomials
                        NS = new ColoredSystem<C>( id, Sp );
                        if ( logger.isDebugEnabled() ) {
                            logger.info("new determined " + NS);
                        }
                        NS = NS.reDetermine();
                        if ( logger.isDebugEnabled() ) {
                            logger.info("re determined " + NS);
                        }
                        //System.out.println("NS = " + NS);
                        NCS.add( NS );
                        //}
                }
            }
        }
        if ( logger.isDebugEnabled() ) {
           logger.info("determined " + NCS);
        }
        return NCS;
    }


    /**
     * Determine polynomial list relative to conditions in F.
     * @param H polynomial list.
     * @return new determined F.
     */
    public List<ColoredSystem<C>> 
        determine( List<ColoredSystem<C>> CS, 
                   List<GenPolynomial<GenPolynomial<C>>> H) {  
        if ( H == null || H.size() == 0 ) {
            return CS;
        }
        List<ColoredSystem<C>> NCS = CS;
        for ( GenPolynomial<GenPolynomial<C>> A : H ) {
            NCS = determine( NCS, A );
        }
        return NCS;
    }


    /**
     * Determine polynomial list.
     * @param H polynomial list.
     * @return new determined list of colored systems.
     */
    public List<ColoredSystem<C>> 
           determine( List<GenPolynomial<GenPolynomial<C>>> H) {  
        if ( H == null || H.size() == 0 ) {
           List<ColoredSystem<C>> CS = new ArrayList<ColoredSystem<C>>();
           return CS;
        }
        List<Ideal<C>> cd = caseDistinction( H );
        return determineCd(cd,H);
    }


    /**
     * Determine polynomial list.
     * @param H polynomial list.
     * @param cd case distiction, an ideal list.
     * @return new determined list of colored systems.
     */
    public List<ColoredSystem<C>> 
           determineCd( List<Ideal<C>> cd,
                        List<GenPolynomial<GenPolynomial<C>>> H) {  
        List<ColoredSystem<C>> CS = new ArrayList<ColoredSystem<C>>();
        if ( H == null || H.size() == 0 ) {
           return CS;
        }
        GenPolynomial<GenPolynomial<C>> zero = H.get(0).ring.getZERO();
        for ( Ideal<C> id : cd ) {
            if ( id.isONE() ) {
               System.out.println("ideal is one = " + id);
               continue; // can treat all coeffs as green
            }
            List<ColorPolynomial<C>> S = new ArrayList<ColorPolynomial<C>>();
            for ( GenPolynomial<GenPolynomial<C>> A : H ) {
                GenPolynomial<GenPolynomial<C>> green = zero;
                GenPolynomial<GenPolynomial<C>> red = zero;
                GenPolynomial<GenPolynomial<C>> white = zero;
                GenPolynomial<GenPolynomial<C>> Ap = A;
                GenPolynomial<GenPolynomial<C>> Bp;
                ColorPolynomial<C> nz;
                while( !Ap.isZERO() ) {
                    Map.Entry<ExpVector,GenPolynomial<C>> m = Ap.leadingMonomial();
                    ExpVector e = m.getKey();
                    GenPolynomial<C> c = m.getValue();
                    Bp = Ap.reductum();
                    if ( c.isConstant() ) {
                        red = zero.sum(c,e);
                        white = Bp;
                        nz = new ColorPolynomial<C>(green,red,white); 
                        System.out.println("nz = " + nz);
                        S.add( nz );
                        break;
                    }
                    if ( id.contains( c ) ) {
                        //System.out.println("c in id = " + c);
                        green = green.sum(c,e);
                        Ap = Bp;
                    } else {
                        red = zero.sum(c,e);
                        white = Bp;
                        nz = new ColorPolynomial<C>(green,red,white); 
                        //System.out.println("nz = " + nz);
                        S.add( nz );
                        break;
                    }
                }
                if ( red.isZERO() ) {
                   if ( !white.isZERO() ) {
                      System.out.println("error, white non zero = " + white);
                   }
                   System.out.println("all green terms, dropped = " + green);
                }
            }
        ColoredSystem<C> cs = new ColoredSystem<C>( id, S );
        //System.out.println("cs = " + cs);
        CS.add( cs );
        }
    return CS;
    }


    /**
     * Determine polynomial relative to conditions in F.
     * @param A color polynomial.
     * @return new F.
     */
    public List<ColoredSystem<C>> 
        determine( ColoredSystem<C> cs, 
                   ColorPolynomial<C> A) {  

        List<Ideal<C>> Il = caseDistinction(cs,A);
        //System.out.println("new case distinction = " + Il);

        List<ColoredSystem<C>> NCS = new ArrayList<ColoredSystem<C>>();
        if ( A == null || A.isZERO() ) {
           //NCS.add( cs );
           return NCS;
        }
        List<ColorPolynomial<C>> S = cs.list;
        Ideal<C> id = cs.conditions; //.clone();
        List<ColorPolynomial<C>> Sp;
        ColorPolynomial<C> nz;
        ColoredSystem<C> NS;
        if ( A.isDetermined() ) {
           Sp = new ArrayList<ColorPolynomial<C>>( S );
           Sp.add( A );
           NS = new ColoredSystem<C>( id, Sp );
           NS = NS.reDetermine();
           NCS.add(NS);
           System.out.println("new determination = " + NCS);
           return NCS;
        }
        GenPolynomial<GenPolynomial<C>> zero = A.green.ring.getZERO();
        GenPolynomial<GenPolynomial<C>> green = A.green;
        GenPolynomial<GenPolynomial<C>> red = A.red; // assert red == 0 
        GenPolynomial<GenPolynomial<C>> white;
        GenPolynomial<GenPolynomial<C>> Ap = A.white;
        GenPolynomial<GenPolynomial<C>> Bp;
        //System.out.println("starting id = " + id);
        while( !Ap.isZERO() ) {
            Map.Entry<ExpVector,GenPolynomial<C>> m = Ap.leadingMonomial();
            ExpVector e = m.getKey();
            GenPolynomial<C> c = m.getValue();
            Bp = Ap.reductum();
            if ( c.isConstant() ) { // already red
                System.out.println("c is constant = " + c);
                red = zero.sum(c,e);
                white = Bp;
                nz = new ColorPolynomial<C>(green,red,white); 
                //System.out.println("nz = " + nz);
                Sp = new ArrayList<ColorPolynomial<C>>( S );
                Sp.add( nz );
                NS = new ColoredSystem<C>( id, Sp );
                NS = NS.reDetermine();
                //System.out.println("NS = " + NS);
                NCS.add( NS );
                break;
            }
            if ( id.contains(c) ) { // already green
                System.out.println("c in id = " + c);
                green = green.sum(c,e);
                Ap = Bp;
                continue;
            }
            red = zero.sum(c,e); // make red
            white = Bp;
            nz = new ColorPolynomial<C>(green,red,white); 
            //System.out.println("nz = " + nz);
            Sp = new ArrayList<ColorPolynomial<C>>( S );
            Sp.add( nz );
            // re determine existing polynomials
            NS = new ColoredSystem<C>( id, Sp );
            NS = NS.reDetermine();
            //System.out.println("NS = " + NS);
            NCS.add( NS );

            id = id.sum( c ); // make green
            //System.out.println("id = " + id);
            if ( id.isONE() ) { // can treat remaining coeffs as green
                //System.out.println("dropping " + cs);
                //System.out.println("by green " + green.sum(c,e));
                break; // drop system
            }
            green = green.sum(c,e);
            Ap = Bp;
        }
        System.out.println("new determination = " + NCS);
        return NCS;
    }


    /**
     * Case distinction ideals of parametric polynomial list.
     * @param L list of parametric polynomials.
     * @return List of case distinction ideals.
     */
    public List<Ideal<C>> caseDistinction( List<GenPolynomial<GenPolynomial<C>>> L) {  
        List<Ideal<C>> cd = new ArrayList<Ideal<C>>();
        if ( L == null || L.size() == 0 ) {
            return cd;
        }
        GenPolynomialRing<GenPolynomial<C>> fac = L.get(0).ring;
        RingFactory<GenPolynomial<C>> crfac = fac.coFac;
        GenPolynomialRing<C> cfac = (GenPolynomialRing<C>) crfac;

        List<GenPolynomial<C>> F = new ArrayList<GenPolynomial<C>>();

        GenPolynomial<GenPolynomial<C>> zero = fac.getZERO();
        GenPolynomial<GenPolynomial<C>> Ap;
        GenPolynomial<GenPolynomial<C>> Bp;
        Ideal<C> I = new Ideal<C>(cfac,F);
        System.out.println("starting I = " + I);
        cd.add(I);
        List<Ideal<C>> C = cd;

        for ( GenPolynomial<GenPolynomial<C>> A : L ) {
            Ap = A;
            while( !Ap.isZERO() ) {
                GenPolynomial<C> c = Ap.leadingBaseCoefficient();
                Bp = Ap.reductum();
                if ( c.isConstant() ) {
                   break;
                }
                C = new ArrayList<Ideal<C>>( cd );
                for ( Ideal<C> id : cd ) {
                    if ( id.contains(c) ) {
                        //System.out.println("c in id = " + c);
                        Ap = Bp;
                        continue;
                    }
                    id = id.sum( c );
                    C.add( id );
                    if ( id.isONE() ) { // can treat remaining coeffs as green
                       System.out.println("dropping " + id);
                       break; // drop system
                    }
                }
                Ap = Bp;
                cd = C;
            }
        }
        //System.out.println("cd = " + cd);
        return cd;
    }


    /**
     * Case distinction ideals of parametric polynomial list.
     * @param L list of parametric polynomials.
     * @param cd colored system.
     * @return List of case distinction ideals.
     */
    public List<Ideal<C>> caseDistinction( ColoredSystem<C> cs,
                                           ColorPolynomial<C> A) {  
        List<Ideal<C>> cd = new ArrayList<Ideal<C>>();
        if ( A == null || A.isZERO() ) {
            return cd;
        }
        if ( A.isDetermined() ) {
           return cd;
        }
        Ideal<C> I = cs.conditions;
        System.out.println("starting I = " + I.list.list);
        cd.add(I);
        List<Ideal<C>> C = new ArrayList<Ideal<C>>( cd );
        List<GenPolynomial<C>> nz = new ArrayList<GenPolynomial<C>>();
        List<List<GenPolynomial<C>>> N = new ArrayList<List<GenPolynomial<C>>>();
        N.add(nz);
        List<List<GenPolynomial<C>>> M = new ArrayList<List<GenPolynomial<C>>>();

        GenPolynomial<GenPolynomial<C>> Ap = A.white;
        GenPolynomial<GenPolynomial<C>> Bp;

        while( !Ap.isZERO() ) {
            GenPolynomial<C> c = Ap.leadingBaseCoefficient();
            System.out.println("next c = " + c);
            Bp = Ap.reductum();
            if ( c.isConstant() ) {
                break;
            }
            C = new ArrayList<Ideal<C>>(cd);
            M = new ArrayList<List<GenPolynomial<C>>>(N);
            Iterator<List<GenPolynomial<C>>> Ni = N.iterator(); 
            for ( Ideal<C> id : cd ) {
                nz = Ni.next();
                System.out.println("id = " + id.list.list);
                System.out.println("nz = " + nz);
                if ( id.contains(c) ) {
                    System.out.println("c in id = " + c);
                    Ap = Bp;
                    continue;
                }
                if ( nz.contains(c) ) {
                    System.out.println("c in nz = " + c);
                    Ap = Bp;
                    continue;
                }
                Ideal<C> idp = id.sum( c );
                System.out.println("make c green = " + c);
                C.add( idp );
                M.add( nz );
                System.out.println("make c red = " + c);
                boolean drin = false;
                for ( GenPolynomial<C> a : nz ) {
                    if ( a.equals(c) ) {
                       drin = true;
                       break;
                    } 
                }
                if ( !drin ) {
                   nz.add( c );
                } 
                C.add( id );
                M.add( nz );
                //if ( id.isONE() ) { // can treat remaining coeffs as green
                //    System.out.println("dropping " + id);
                //    break; // drop system
                //}
            }
            Ap = Bp;
            cd = C;
            N = M;
            System.out.println("x cd = " + cd);
            System.out.println("x N  = " + N);
        }

        System.out.println("new case distinction:");
        System.out.println("cd = " + cd);
        System.out.println("N  = " + N);
        return cd;
    }

}
