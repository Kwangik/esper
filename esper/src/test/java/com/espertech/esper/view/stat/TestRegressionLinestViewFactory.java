/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.view.stat;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.epl.SupportExprNodeFactory;
import com.espertech.esper.support.event.SupportEventTypeFactory;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.view.TestViewSupport;
import com.espertech.esper.view.ViewFactoryContext;
import com.espertech.esper.view.ViewFieldEnum;
import com.espertech.esper.view.ViewParameterException;
import com.espertech.esper.view.std.FirstElementView;
import junit.framework.TestCase;

import java.util.Arrays;

public class TestRegressionLinestViewFactory extends TestCase
{
    private RegressionLinestViewFactory factory;
    private ViewFactoryContext viewFactoryContext = new ViewFactoryContext(null, 1, null, null, false, -1, false);

    public void setUp()
    {
        factory = new RegressionLinestViewFactory();
    }

    public void testSetParameters() throws Exception
    {
        tryParameter(new Object[] {"price", "volume"}, "price", "volume");

        tryInvalidParameter(new Object[] {"symbol", 1.1d});
        tryInvalidParameter(new Object[] {1.1d, "symbol"});
        tryInvalidParameter(new Object[] {1.1d});
        tryInvalidParameter(new Object[] {new String[] {"a", "b"}});
    }

    public void testCanReuse() throws Exception
    {
        factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListMD(new Object[] {"price", "volume"}));
        factory.attach(SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class), SupportStatementContextFactory.makeContext(), null, null);
        assertFalse(factory.canReuse(new FirstElementView(null)));
        EventType type = RegressionLinestView.createEventType(SupportStatementContextFactory.makeContext(), null, 1);
        assertFalse(factory.canReuse(new RegressionLinestView(null, SupportStatementContextFactory.makeAgentInstanceContext(), SupportExprNodeFactory.makeIdentNodeMD("price"), SupportExprNodeFactory.makeIdentNodeMD("price"), type, null)));
        assertFalse(factory.canReuse(new RegressionLinestView(null, SupportStatementContextFactory.makeAgentInstanceContext(), SupportExprNodeFactory.makeIdentNodeMD("volume"), SupportExprNodeFactory.makeIdentNodeMD("price"), type, null)));
        assertTrue(factory.canReuse(new RegressionLinestView(null, SupportStatementContextFactory.makeAgentInstanceContext(), SupportExprNodeFactory.makeIdentNodeMD("price"), SupportExprNodeFactory.makeIdentNodeMD("volume"), type, null)));
    }

    public void testAttaches() throws Exception
    {
        // Should attach to anything as long as the fields exists
        EventType parentType = SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class);

        factory.setViewParameters(viewFactoryContext,
                Arrays.asList(new ExprNode[] {SupportExprNodeFactory.makeIdentNodeMD("volume"), SupportExprNodeFactory.makeIdentNodeMD("price")}));
        factory.attach(parentType, SupportStatementContextFactory.makeContext(), null, null);
        assertEquals(Double.class, factory.getEventType().getPropertyType(ViewFieldEnum.REGRESSION__SLOPE.getName()));

        try
        {
            factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListMD(new Object[] {"symbol", "symbol"}));
            factory.attach(parentType, SupportStatementContextFactory.makeContext(), null, null);
            fail();
        }
        catch (ViewParameterException ex)
        {
            // expected;
        }
    }

    private void tryInvalidParameter(Object[] parameters) throws Exception
    {
        try
        {
            factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListMD(parameters));
            factory.attach(SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class), SupportStatementContextFactory.makeContext(), null, null);
            fail();
        }
        catch (ViewParameterException ex)
        {
            // expected
        }
    }

    private void tryParameter(Object[] parameters, String fieldNameX, String fieldNameY) throws Exception
    {
        factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListMD(parameters));
        factory.attach(SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class), SupportStatementContextFactory.makeContext(), null, null);
        RegressionLinestView view = (RegressionLinestView) factory.makeView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext());
        assertEquals(fieldNameX, ExprNodeUtility.toExpressionStringMinPrecedenceSafe(view.getExpressionX()));
        assertEquals(fieldNameY, ExprNodeUtility.toExpressionStringMinPrecedenceSafe(view.getExpressionY()));
    }
}
