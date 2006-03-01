/*
 * $Id$
 */

package edu.jas.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.io.Serializable;

import org.apache.log4j.Logger;

import edu.jas.structure.RingElem;

import edu.jas.poly.ExpVector;
import edu.jas.poly.GenPolynomial;
import edu.jas.poly.GenPolynomialRing;
import edu.jas.poly.PolynomialList;

import edu.jas.ring.ExtendedGB;
import edu.jas.ring.GroebnerBase;
import edu.jas.ring.Reduction;
import edu.jas.ring.GroebnerBaseSeqPairSeq;
import edu.jas.ring.ReductionSeq;


/**
 * Ideal. Some methods for ideal arithmetic.
 * @author Heinz Kredel
 */
public class Ideal<C extends RingElem<C>> implements Serializable {


  private static Logger logger = Logger.getLogger(Ideal.class);
  private boolean debug = logger.isDebugEnabled();



  /** 
   * The data structure is a PolynomialList. 
   */
  protected PolynomialList<C> list;


  /** 
   * Indicator if list is a Groebner Base. 
   */
  protected boolean isGB;


  /** 
   * Indicator if test has been performed if this is a Groebner Base. 
   */
  protected boolean testGB;


  /** 
   * Groebner base engine. 
   */
  protected GroebnerBase<C> bb;


  /**
   * Reduction engine.
   */
  protected Reduction<C> red;


  /**
   * Constructor.
   * @param ring polynomial ring
   * @param F list of polynomials
   */
  public Ideal( GenPolynomialRing<C> ring, List<GenPolynomial<C>> F ) {
      this( new PolynomialList<C>( ring, F ) );
  }


  /**
   * Constructor.
   * @param ring polynomial ring
   * @param F list of polynomials
   * @param gb true if F is known to be a Groebner Base, else false
   */
  public Ideal( GenPolynomialRing<C> ring, 
                List<GenPolynomial<C>> F, boolean gb ) {
      this( new PolynomialList<C>( ring, F ), gb );
  }


  /**
   * Constructor.
   * @param list polynomial list
   */
  public Ideal( PolynomialList<C> list) {
      this( list, 
            ( list == null ? true : ( list.list == null ? true : false ) ) );
  }


  /**
   * Constructor.
   * @param list polynomial list
   * @param bb Groebner Base engine
   * @param red Reduction engine
   */
  public Ideal( PolynomialList<C> list,
                GroebnerBase<C> bb, Reduction<C> red) {
      this( list, 
            ( list == null ? true : ( list.list == null ? true : false ) ), 
            new GroebnerBaseSeqPairSeq<C>(), 
            new ReductionSeq<C>() );
  }


  /**
   * Constructor.
   * @param list polynomial list
   * @param gb true if list is known to be a Groebner Base, else false
   */
  public Ideal( PolynomialList<C> list, boolean gb) {
      this(list,gb, new GroebnerBaseSeqPairSeq<C>(), new ReductionSeq<C>() );
  }


  /**
   * Constructor.
   * @param list polynomial list
   * @param gb true if list is known to be a Groebner Base, else false
   * @param bb Groebner Base engine
   * @param red Reduction engine
   */
    public Ideal( PolynomialList<C> list, boolean gb,
                  GroebnerBase<C> bb, Reduction<C> red) {
      this.list = list;
      this.isGB = gb;
      this.testGB = ( gb ? true : false ); // ??
      this.bb = bb;
      this.red = red;
  }


  /**
   * Get the List of GenPolynomials.
   * @return list.list
   */
  public List< GenPolynomial<C> > getList() {
      if ( list == null ) {
          return null;
      }
      return list.list;
  }


  /**
   * Get the GenPolynomialRing.
   * @return list.ring
   */
  public GenPolynomialRing<C> getRing() {
      if ( list == null ) {
          return null;
      }
      return list.ring;
  }


  /**
   * String representation of the ideal.
   * @see java.lang.Object#toString()
   */
  public String toString() {
      return list.toString();
  }


  /** Comparison with any other object.
   * Note: If both ideals are not Groebner Bases, then
   *       false may be returned even the ideals are equal.
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  @SuppressWarnings("unchecked") // not jet working
  public boolean equals(Object b) {
      if ( ! (b instanceof Ideal) ) {
         logger.warn("equals no Ideal");
         return false;
      }
      Ideal<C> B = null;
      try {
          B = (Ideal<C>)b;
      } catch (ClassCastException ignored) {
      }
      if ( isGB && B.isGB ) {
         return list.equals( B.list );
      } else { // compute GBs ?
         return list.equals( B.list );
      }
  }


  /**
   * Test if ZERO ideal.
   * @return true, if this is the 0 ideal, else false
   */
  public boolean isZERO() {
      return list.isZERO();
  }


  /**
   * Test if ONE ideal.
   * @return true, if this is the 1 ideal, else false
   */
  public boolean isONE() {
      return list.isONE();
  }


  /**
   * Test if this is a Groebner base.
   * @return true, if this is a Groebner base, else false
   */
  public boolean isGB() {
      if ( testGB ) {
          return isGB;
      }
      logger.warn("isGB computing");
      isGB = bb.isGB( getList() );
      testGB = true;
      return isGB;
  }


  /**
   * Groebner Base. Get a Groebner Base for this ideal.
   * @return GB(this)
   */
  public Ideal<C> GB() {
      if ( isGB ) {
          return this;
      }
      logger.warn("GB computing");
      List< GenPolynomial<C> > c = getList();
      c = bb.GB( c );
      return new Ideal<C>( getRing(), c, true );
  }


  /**
   * Ideal containment. Test if B is contained in this ideal.
   * Note: this is eventually modified to become a Groebner Base.
   * @param B ideal
   * @return true, if B is contained in this, else false
   */
  public boolean contains( Ideal<C> B ) {
      if ( B == null ) {
          return true;
      }
      if ( B.isZERO() ) {
          return true;
      }
      if ( this.isONE() ) {
          return true;
      }
      if ( !isGB ) {
         logger.warn("contains computing GB");
         List< GenPolynomial<C> > c = getList();
         c = bb.GB( c );
         list = new PolynomialList<C>( getRing(), c );
         isGB = true;
         testGB = true;
      }
      List< GenPolynomial<C> > z;
      z = red.normalform( getList(), B.getList() );
      if ( z == null ) {
          return true;
      }
      for ( GenPolynomial<C> p : z ) {
          if ( p == null ) {
              continue;
          }
          if ( ! p.isZERO() ) {
             return false;
          }
      }
      return true;
  }


  /**
   * Summation. Generators for the sum of ideals.
   * Note: if both ideals are Groebner bases, a Groebner base is returned.
   * @param B ideal
   * @return ideal(this+B)
   */
  public Ideal<C> sum( Ideal<C> B ) {
      if ( B == null ) {
          return this;
      }
      if ( B.isZERO() ) {
          return this;
      }
      if ( this.isZERO() ) {
          return B;
      }
      int s = getList().size() + B.getList().size();
      List< GenPolynomial<C> > c;
      c = new ArrayList<GenPolynomial<C>>( s );
      c.addAll( getList() );
      c.addAll( B.getList() );
      if ( isGB && B.isGB ) {
         logger.warn("sum computing GB");
         c = bb.GB( c );
         return new Ideal<C>( getRing(), c, true );
      } else {
         return new Ideal<C>( getRing(), c, false );
      }
  }


  /**
   * Product. Generators for the product of ideals.
   * Note: if both ideals are Groebner bases, a Groebner base is returned.
   * @param B ideal
   * @return ideal(this*B)
   */
  public Ideal<C> product( Ideal<C> B ) {
      if ( B == null ) {
          return B;
      }
      if ( B.isZERO() ) {
          return B;
      }
      if ( this.isZERO() ) {
          return this;
      }
      int s = getList().size() * B.getList().size();
      List< GenPolynomial<C> > c;
      c = new ArrayList<GenPolynomial<C>>( s );
      for ( GenPolynomial<C> p : getList() ) {
          for ( GenPolynomial<C> q : B.getList() ) {
              q = p.multiply(q);
              c.add(q);
          }
      }
      if ( isGB && B.isGB ) {
         logger.warn("product computing GB");
         c = bb.GB( c );
         return new Ideal<C>( getRing(), c, true );
      } else {
         return new Ideal<C>( getRing(), c, false );
      }
  }


  /**
   * Intersection. Generators for the intersection of ideals.
   * @param B ideal
   * @return ideal(this \cap B), a Groebner base
   */
  public Ideal<C> intersect( Ideal<C> B ) {
      if ( B == null ) { // == (0)
          return B;
      }
      if ( B.isZERO() ) { 
          return B;
      }
      if ( this.isZERO() ) { 
          return this;
      }
      int s = getList().size() + B.getList().size();
      List< GenPolynomial<C> > c;
      c = new ArrayList<GenPolynomial<C>>( s );
      List< GenPolynomial<C> > a = getList();
      List< GenPolynomial<C> > b = B.getList();

      GenPolynomialRing<C> tfac = getRing().extend(1);
      // term order is also adjusted
      for ( GenPolynomial<C> p : a ) {
          p = p.extend( tfac, 0, 1L ); // t*p
          c.add( p );
      }
      for ( GenPolynomial<C> p : b ) {
          GenPolynomial<C> q = p.extend( tfac, 0, 1L );
          GenPolynomial<C> r = p.extend( tfac, 0, 0L );
          p = r.subtract( q ); // (1-t)*p
          c.add( p );
      }
      logger.warn("intersect computing GB");
      List< GenPolynomial<C> > g = bb.GB( c );
      if ( debug ) {
         logger.debug("intersect GB = " + g);
      }
      Ideal<C> E = new Ideal<C>( tfac, g, true );
      Ideal<C> I = E.intersect( getRing() );
      return I;
  }


  /**
   * Intersection. Generators for the intersection of a ideal 
   * with a polynomial ring. The polynomial ring of this ideal
   * must be a contraction of R and the TermOrder must be 
   * an elimination order.
   * @param R polynomial ring
   * @return ideal(this \cap R)
   */
  public Ideal<C> intersect( GenPolynomialRing<C> R ) {
      if ( R == null ) { 
         throw new RuntimeException("R may not be null");
      }
      int d = getRing().nvar - R.nvar;
      if ( d <= 0 ) {
          return this;
      }
      //GenPolynomialRing<C> tfac = getRing().contract(d);
      //if ( ! tfac.equals( R ) ) { // check ?
      //   throw new RuntimeException("contract(this) != R");
      //}
      List< GenPolynomial<C> > h;
      h = new ArrayList<GenPolynomial<C>>( getList().size() );
      for ( GenPolynomial<C> p : getList() ) {
          Map<ExpVector,GenPolynomial<C>> m = null;
          m = p.contract( R );
          if ( debug ) {
             logger.debug("intersect contract m = " + m);
          }
          if ( m.size() == 1 ) { // contains one power of variables
             for ( ExpVector e : m.keySet() ) {
                 if ( e.isZERO() ) {
                    h.add( m.get( e ) );
                 }
             }
          }
      }
      return new Ideal<C>( R, h, isGB );
  }


  /**
   * Quotient. Generators for the ideal quotient.
   * @param h polynomial
   * @return ideal(this : h), a Groebner base
   */
  public Ideal<C> quotient( GenPolynomial<C> h ) {
      if ( h == null ) { // == (0)
          return this;
      }
      if ( h.isZERO() ) { 
          return this;
      }
      if ( this.isZERO() ) { 
          return this;
      }
      List< GenPolynomial<C> > H;
      H = new ArrayList<GenPolynomial<C>>( 1 );
      H.add( h );
      Ideal<C> Hi = new Ideal<C>( getRing(), H, true );

      Ideal<C> I = this.intersect( Hi );

      List< GenPolynomial<C> > Q;
      Q = new ArrayList<GenPolynomial<C>>( I.getList().size() );
      for ( GenPolynomial<C> q : I.getList() ) {
          q = q.divide(h); // remainder == 0
          Q.add( q );
      }
      return new Ideal<C>( getRing(), Q, true /*false?*/ );
  }


  /**
   * Quotient. Generators for the ideal quotient.
   * @param H ideal
   * @return ideal(this : H), a Groebner base
   */
  public Ideal<C> quotient( Ideal<C> H ) {
      if ( H == null ) { // == (0)
          return this;
      }
      if ( H.isZERO() ) { 
          return this;
      }
      if ( this.isZERO() ) { 
          return this;
      }
      Ideal<C> Q = null;
      for ( GenPolynomial<C> h : H.getList() ) {
          Ideal<C> Hi = this.quotient(h);
          if ( Q == null ) {
             Q = Hi;
          } else {
             Q = Q.intersect( Hi );
          }
      }
      return Q;
  }


  /**
   * Infinite quotient. Generators for the infinite ideal quotient.
   * @param h polynomial
   * @return ideal(this : h<sup>s</sup>), a Groebner base
   */
  public Ideal<C> infiniteQuotient( GenPolynomial<C> h ) {
      if ( h == null ) { // == (0)
          return this;
      }
      if ( h.isZERO() ) { 
          return this;
      }
      if ( this.isZERO() ) {
          return this;
      }
      int s = 0;
      Ideal<C> I = this.GB(); // should be already
      GenPolynomial<C> hs = h;

      boolean eq = false;
      while ( !eq ) {
        Ideal<C> Is = I.quotient( hs );
        Is = Is.GB(); // should be already
        logger.info("infiniteQuotient s = " + s);
        eq = Is.contains(I);  // I.contains(Is) always
        if ( !eq ) {
           I = Is;
           s++;
           // hs = hs.multiply( h );
        }
      }
      return I;
  }


  /**
   * Infinite quotient. Generators for the infinite ideal quotient.
   * @param h polynomial
   * @return ideal(this : h<sup>s</sup>), a Groebner base
   */
  public Ideal<C> infiniteQuotientOld( GenPolynomial<C> h ) {
      if ( h == null ) { // == (0)
          return this;
      }
      if ( h.isZERO() ) { 
          return this;
      }
      if ( this.isZERO() ) {
          return this;
      }
      int s = 0;
      Ideal<C> I = this.GB(); // should be already
      GenPolynomial<C> hs = h;

      boolean eq = false;
      while ( !eq ) {
        Ideal<C> Is = I.quotient( hs );
        Is = Is.GB(); // should be already
        logger.debug("infiniteQuotient s = " + s);
        eq = Is.contains(I);  // I.contains(Is) always
        if ( !eq ) {
           I = Is;
           s++;
           hs = hs.multiply( h );
        }
      }
      return I;
  }


  /**
   * Infinite Quotient. Generators for the ideal infinite  quotient.
   * @param H ideal
   * @return ideal(this : H<sup>s</sup>), a Groebner base
   */
  public Ideal<C> infiniteQuotient( Ideal<C> H ) {
      if ( H == null ) { // == (0)
          return this;
      }
      if ( H.isZERO() ) { 
          return this;
      }
      if ( this.isZERO() ) { 
          return this;
      }
      Ideal<C> Q = null;
      for ( GenPolynomial<C> h : H.getList() ) {
          Ideal<C> Hi = this.infiniteQuotient(h);
          if ( Q == null ) {
             Q = Hi;
          } else {
             Q = Q.intersect( Hi );
          }
      }
      return Q;
  }


  /**
   * Normalform for element.
   * @param h polynomial
   * @return normalform of h with respect to this
   */
  public GenPolynomial<C> normalform( GenPolynomial<C> h ) {
      if ( h == null ) { 
          return h;
      }
      if ( h.isZERO() ) { 
          return h;
      }
      if ( this.isZERO() ) { 
          return h;
      }
      GenPolynomial<C> r;
      r = red.normalform( list.list, h );
      return r;
  }


  /**
   * Inverse for element modulo this ideal.
   * @param h polynomial
   * @return inverse of h with respect to this, if defined
   */
  public GenPolynomial<C> inverse( GenPolynomial<C> h ) {
      if ( h == null || h.isZERO() ) { 
         throw new RuntimeException(this.getClass().getName()
                                    + " not invertible");
      }
      if ( this.isZERO() ) { 
         throw new RuntimeException(this.getClass().getName()
                                    + " not invertible");
      }
      List<GenPolynomial<C>> F = new ArrayList<GenPolynomial<C>>( 1 + list.list.size() );
      F.add( h );
      F.addAll( list.list );
      ExtendedGB<C> x = bb.extGB( F );
      List<GenPolynomial<C>> G = x.G;
      GenPolynomial<C> one = null;
      int i = -1;
      for ( GenPolynomial<C> p : G ) {
          i++;
          if ( p == null ) {
             continue;
          }
          if ( p.isUnit() ) {
             one = p;
             break;
          }
      }
      if ( one == null ) { 
         throw new RuntimeException(this.getClass().getName()
                                    + " not invertible");
      }
      List<GenPolynomial<C>> row = x.G2F.get( i ); // != -1
      GenPolynomial<C> g = row.get(0);
      if ( g == null || g.isZERO() ) { 
         throw new RuntimeException(this.getClass().getName()
                                    + " not invertible " + h);
      }
      // adjust g to get g*h == 1
      GenPolynomial<C> f = g.multiply(h);
      GenPolynomial<C> k = red.normalform(list.list,f);
      if ( ! k.isONE() ) {
         C lbc = k.leadingBaseCoefficient();
         lbc = lbc.inverse();
         g = g.multiply( lbc );
      }
      if ( debug ) {
         //logger.info("inv G = " + G);
         //logger.info("inv G2F = " + x.G2F);
         //logger.info("inv row "+i+" = " + row);
         //logger.info("inv h = " + h);
         //logger.info("inv g = " + g);
         //logger.info("inv f = " + f);
         f = g.multiply(h);
         k = red.normalform(list.list,f);
         logger.info("inv k = " + k);
         if ( ! k.isUnit() ) {
            throw new RuntimeException(this.getClass().getName()
                                       + " not invertible");
         }
      }
      return g;
  }


  /**
   * Test if element is a unit modulo this ideal.
   * @param h polynomial
   * @return true if h is a unit with respect to this, else false
   */
  public boolean isUnit( GenPolynomial<C> h ) {
      if ( h == null || h.isZERO() ) { 
         return false;
      }
      if ( this.isZERO() ) { 
         return false;
      }
      List<GenPolynomial<C>> F = new ArrayList<GenPolynomial<C>>( 1 + list.list.size() );
      F.add( h );
      F.addAll( list.list );
      List<GenPolynomial<C>> G = bb.GB( F );
      for ( GenPolynomial<C> p : G ) {
          if ( p == null ) {
             continue;
          }
          if ( p.isUnit() ) {
             return true;
          }
      }
      return false;
  }


}
