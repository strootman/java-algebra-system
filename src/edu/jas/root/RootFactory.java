/*
 * $Id$
 */

package edu.jas.root;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.jas.arith.BigRational;

import edu.jas.poly.GenPolynomial;
import edu.jas.poly.PolyUtil;

import edu.jas.structure.RingElem;
import edu.jas.structure.GcdRingElem;
import edu.jas.structure.RingFactory;
import edu.jas.structure.UnaryFunctor;

import edu.jas.ufd.GreatestCommonDivisor;
import edu.jas.ufd.GCDFactory;
import edu.jas.ufd.FactorFactory;
import edu.jas.ufd.FactorAbstract;


/**
 * Roots factory.
 * @param <C> coefficient type.
 * @author Heinz Kredel
 */
public class RootFactory {


    /**
     * Real algebraic numbers.
     * @param f univariate polynomial.
     * @return a list of different real algebraic numbers.
     */
    public static <C extends GcdRingElem<C>>
                  List<RealAlgebraicNumber<C>> 
      realAlgebraicNumbers(GenPolynomial<C> f) {
        RealRoots<C> rr = new RealRootsSturm<C>();
        GreatestCommonDivisor<C> engine = GCDFactory.<C>getProxy(f.ring.coFac);
        Set<GenPolynomial<C>> S = engine.squarefreeFactors(f).keySet();
        List<RealAlgebraicNumber<C>> list = new ArrayList<RealAlgebraicNumber<C>>();
        for ( GenPolynomial<C> sp : S ) {
            List<Interval<C>> iv = rr.realRoots(sp);
            for ( Interval<C> I : iv ) {
                RealAlgebraicRing<C> rar = new RealAlgebraicRing<C>(sp,I);
                RealAlgebraicNumber<C> rn = rar.getGenerator();
                list.add(rn);
            } 
        }
        return list;
    }


    /**
     * Real algebraic numbers from a field.
     * @param f univariate polynomial.
     * @return a list of different real algebraic numbers from a field.
     */
    public static <C extends GcdRingElem<C>>
                  List<RealAlgebraicNumber<C>> 
      realAlgebraicNumbersField(GenPolynomial<C> f) {
        RealRoots<C> rr = new RealRootsSturm<C>();
        FactorAbstract<C> engine = FactorFactory.<C> getImplementation(f.ring.coFac);
        Set<GenPolynomial<C>> S = engine.baseFactors(f).keySet();
        List<RealAlgebraicNumber<C>> list = new ArrayList<RealAlgebraicNumber<C>>();
        for ( GenPolynomial<C> sp : S ) {
            List<Interval<C>> iv = rr.realRoots(sp);
            for ( Interval<C> I : iv ) {
                RealAlgebraicRing<C> rar = new RealAlgebraicRing<C>(sp,I,true);//field
                RealAlgebraicNumber<C> rn = rar.getGenerator();
                list.add(rn);
            } 
        }
        return list;
    }

}