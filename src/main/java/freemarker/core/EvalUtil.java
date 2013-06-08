/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.core;

import java.util.Date;

import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.utility.ClassUtil;

/**
 * Internally used static utilities for evaluation expressions.
 */
class EvalUtil
{
    static final int CMP_OP_EQUALS = 1;
    static final int CMP_OP_NOT_EQUALS = 2;
    static final int CMP_OP_LESS_THAN = 3;
    static final int CMP_OP_GREATER_THAN = 4;
    static final int CMP_OP_LESS_THAN_EQUALS = 5;
    static final int CMP_OP_GREATER_THAN_EQUALS = 6;
    // If you add a new operator here, update the "compare" and "cmpOpToString" methods!

    // Prevents instantination.
    private EvalUtil() { }
    
    /**
     * @param expr {@code null} is allowed, but may results in less helpful error messages
     * @param env {@code null} is allowed, but may results in less helpful error messages
     */
    static String modelToString(TemplateScalarModel model, Expression expr, Environment env)
    throws
        TemplateException
    {
        String value = model.getAsString();
        if(value == null)
        {
            if(env.isClassicCompatible())
            {
                return "";
            }
            else
            {
                throw newModelHasStoredNullException(String.class, model, expr, env);
            }
        }
        return value;
    }

    /**
     * @param expr {@code null} is allowed, but may results in less helpful error messages
     * @param env {@code null} is allowed, but may results in less helpful error messages
     */
    static Number modelToNumber(TemplateNumberModel model, Expression expr, Environment env)
        throws TemplateModelException, TemplateException
    {
        Number value = model.getAsNumber();
        if(value == null) throw newModelHasStoredNullException(Number.class, model, expr, env);
        return value;
    }

    /**
     * @param expr {@code null} is allowed, but may results in less helpful error messages
     * @param env {@code null} is allowed, but may results in less helpful error messages
     */
    static Date modelToDate(TemplateDateModel model, Expression expr, Environment env)
        throws TemplateModelException, TemplateException
    {
        Date value = model.getAsDate();
        if(value == null) throw newModelHasStoredNullException(Date.class, model, expr, env);
        return value;
    }
    
    /** Handles the buggy case where we have a non-null model, but its wraps a null. */
    private static TemplateException newModelHasStoredNullException(
            Class expected, TemplateModel tm, Expression expr, Environment env) {
        String msg = "The FreeMarker value exists, but has nothing inside it; the TemplateModel object (class: "
                +  tm.getClass().getName() + ") has returned a null instead of a " + expected.getName() + ". "
                + "This is probably a bug in the non-FreeMarker code that builds the data-model.";
        return expr != null ? expr.newTemplateException(msg) : new TemplateException(msg, env);
    }
    
    /**
     * Compares two expressions according the rules of the FTL comparator operators.
     * 
     * @param leftExp not {@code null}
     * @param operator one of the {@code COMP_OP_...} constants, like {@link #CMP_OP_EQUALS}.
     * @param operatorString can be null {@code null}; the actual operator used, used for more accurate error message.
     * @param rightExp not {@code null}
     * @param env {@code null} is tolerated, but should be avoided
     */
    static boolean compare(
            Expression leftExp,
            int operator, String  operatorString,
            Expression rightExp,
            Expression defaultBlamed,
            Environment env) throws TemplateException {
        TemplateModel ltm = leftExp.eval(env);
        TemplateModel rtm = rightExp.eval(env);
        return compare(
                ltm, leftExp,
                operator, operatorString,
                rtm, rightExp,
                defaultBlamed,
                false, false, false,
                env);
    }
    
    /**
     * Compares values according the rules of the FTL comparator operators; if the {@link Expression}-s are
     * accessible, use {@link #compare(Expression, int, String, Expression, Expression, Environment)} instead, as
     * that gives better error messages.
     * 
     * @param leftValue maybe {@code null}, which will usually cause the appropriate {@link TemplateException}. 
     * @param operator one of the {@code COMP_OP_...} constants, like {@link #CMP_OP_EQUALS}.
     * @param rightValue maybe {@code null}, which will usually cause the appropriate {@link TemplateException}.
     * @param env {@code null} is tolerated, but should be avoided
     */
    static boolean compare(
            TemplateModel leftValue, int operator, TemplateModel rightValue,
            Environment env) throws TemplateException {
        return compare(
                leftValue, null,
                operator, null,
                rightValue, null,
                null,
                false, false, false,
                env);
    }

    /**
     * Same as {@link #compare(TemplateModel, int, TemplateModel, Environment)}, but if the two types are incompatible,
     *     they are treated as non-equal instead of throwing an exception. Comparing dates of different types will
     *     still throw an exception, however.
     */
    static boolean compareLenient(
            TemplateModel leftValue, int operator, TemplateModel rightValue,
            Environment env) throws TemplateException {
        return compare(
                leftValue, null,
                operator, null,
                rightValue, null,
                null,
                true, false, false,
                env);
    }
    
    private static final String DATE_OF_THE_COMPARISON_IS_OF_TYPE_UNKNOWN
            = "date of the comparison is of UNKNOWN type (it's not known if it's date-only, time-only, or date-time), "
              + "and thus can't be used in a comparison.";
    
    /**
     * @param leftExp {@code null} is allowed, but may results in less helpful error messages
     * @param operator one of the {@code COMP_OP_...} constants, like {@link #CMP_OP_EQUALS}.
     * @param operatorString can be null {@code null}; the actual operator used, used for more accurate error message.
     * @param rightExp {@code null} is allowed, but may results in less helpful error messages
     * @param defaultBlamed {@code null} allowed; the expression who to which error will point to if something goes
     *        wrong that is not specific to the left or right side expression, or if that expression is {@code null}.
     * @param typeMismatchMeansNotEqual If the two types are incompatible, they are treated as non-equal instead
     *     of throwing an exception. Comparing dates of different types will still throw an exception, however. 
     * @param leftNullReturnsFalse if {@code true}, a {@code null} left value will not cause exception, but make the
     *     expression {@code false}.  
     * @param rightNullReturnsFalse if {@code true}, a {@code null} right value will not cause exception, but make the
     *     expression {@code false}.  
     */
    static boolean compare(
            TemplateModel leftValue, Expression leftExp,
            int operator, String operatorString,
            TemplateModel rightValue, Expression rightExp,
            Expression defaultBlamed,
            boolean typeMismatchMeansNotEqual,
            boolean leftNullReturnsFalse, boolean rightNullReturnsFalse,
            Environment env) throws TemplateException {
        if (leftValue == null) {
            if (env != null && env.isClassicCompatible()) {
                leftValue = TemplateScalarModel.EMPTY_STRING;
            } else {
                if (leftNullReturnsFalse) { 
                    return false;
                } else {
                    if (leftExp != null) {
                        leftExp.assertNonNull(leftValue);
                    } else {
                        String desc = "The left operand of the comparison was undefined or null.";
                        if (defaultBlamed != null) {
                            throw defaultBlamed.newTemplateException(desc);
                        } else {
                            throw new TemplateException(desc, env);
                        }
                    }
                }
            }
        }

        if (rightValue == null) {
            if (env != null && env.isClassicCompatible()) {
                rightValue = TemplateScalarModel.EMPTY_STRING;
            } else {
                if (rightNullReturnsFalse) { 
                    return false;
                } else {
                    if (rightExp != null) {
                        throw rightExp.newInvalidReferenceException();
                    } else {
                        String msg = "The right operand of the comparison was undefined or null.";
                        if (defaultBlamed != null) {
                            throw defaultBlamed.newTemplateException(msg);
                        } else {
                            throw new TemplateException(msg, env);
                        }
                    }
                }
            }
        }

        final int cmpResult;
        if (leftValue instanceof TemplateNumberModel && rightValue instanceof TemplateNumberModel) {
            Number leftNum = EvalUtil.modelToNumber((TemplateNumberModel) leftValue, leftExp, env);
            Number rightNum = EvalUtil.modelToNumber((TemplateNumberModel) rightValue, rightExp, env);
            ArithmeticEngine ae =
                    env != null
                        ? env.getArithmeticEngine()
                        : (leftExp != null
                            ? leftExp.getTemplate().getArithmeticEngine()
                            : ArithmeticEngine.BIGDECIMAL_ENGINE);
            try {
                cmpResult = ae.compareNumbers(leftNum, rightNum);
            } catch (RuntimeException e) {
                String desc = "Unexpected error while comparing two numbers: " + e;
                if (defaultBlamed != null) {
                    throw defaultBlamed.newTemplateModelException(desc, e);
                } else {
                    throw new TemplateModelException(desc, e);
                }
            }
        } else if (leftValue instanceof TemplateDateModel && rightValue instanceof TemplateDateModel) {
            TemplateDateModel leftDateModel = (TemplateDateModel) leftValue;
            TemplateDateModel rightDateModel = (TemplateDateModel) rightValue;
            
            int leftDateType = leftDateModel.getDateType();
            int rightDateType = rightDateModel.getDateType();
            
            if (leftDateType == TemplateDateModel.UNKNOWN || rightDateType == TemplateDateModel.UNKNOWN) {
                String sideName;
                Expression sideExp;
                if (leftDateType == TemplateDateModel.UNKNOWN) {
                    sideName = "left";
                    sideExp = leftExp;
                } else {
                    sideName = "right";
                    sideExp = rightExp;
                }
                
                String desc = "The " + sideName + " " + DATE_OF_THE_COMPARISON_IS_OF_TYPE_UNKNOWN; 
                if (sideExp != null) {
                    throw sideExp.newTemplateException(desc, MessageUtil.UNKNOWN_DATE_TYPE_ERROR_TIPS);                    
                } else if (defaultBlamed != null) {
                    throw defaultBlamed.newTemplateModelException(desc, MessageUtil.UNKNOWN_DATE_TYPE_ERROR_TIPS);
                } else {
                    throw new TemplateException(desc, env);
                }
            }
            
            if (leftDateType != rightDateType) {
                String desc = "Can not compare dates of different types. Left date is of "
                        + TemplateDateModel.TYPE_NAMES.get(leftDateType) + " type, right date is of "
                        + TemplateDateModel.TYPE_NAMES.get(rightDateType) + " type.";
                if (defaultBlamed != null) {
                    throw defaultBlamed.newTemplateModelException(desc);
                } else {
                    throw new TemplateException(desc, env);
                }
            }

            Date leftDate = EvalUtil.modelToDate(leftDateModel, leftExp, env);
            Date rightDate = EvalUtil.modelToDate(rightDateModel, rightExp, env);
            cmpResult = leftDate.compareTo(rightDate);
        } else if (leftValue instanceof TemplateScalarModel && rightValue instanceof TemplateScalarModel) {
            if (operator != CMP_OP_EQUALS && operator != CMP_OP_NOT_EQUALS) {
                String desc = "Can not use operator \"" + cmpOpToString(operator, operatorString)
                        + "\" on string values.";
                if (defaultBlamed != null) {
                    throw defaultBlamed.newTemplateModelException(desc);
                } else {
                    throw new TemplateException(desc, env);
                }
            }
            String leftString = EvalUtil.modelToString((TemplateScalarModel) leftValue, leftExp, env);
            String rightString = EvalUtil.modelToString((TemplateScalarModel) rightValue, rightExp, env);
            // FIXME NBC: Don't use the Collator here. That's locale-specific, but ==/!= should not be.
            cmpResult = env.getCollator().compare(leftString, rightString);
        } else if (leftValue instanceof TemplateBooleanModel && rightValue instanceof TemplateBooleanModel) {
            if (operator != CMP_OP_EQUALS && operator != CMP_OP_NOT_EQUALS) {
                String desc = "Can not use operator \"" + cmpOpToString(operator, operatorString)
                        + "\" on boolean values.";
                if (defaultBlamed != null) {
                    throw defaultBlamed.newTemplateModelException(desc);
                } else {
                    throw new TemplateException(desc, env);
                }
            }
            boolean leftBool = ((TemplateBooleanModel) leftValue).getAsBoolean();
            boolean rightBool = ((TemplateBooleanModel) rightValue).getAsBoolean();
            cmpResult = (leftBool ? 1 : 0) - (rightBool ? 1 : 0);
        } else if (env.isClassicCompatible()) {
            String leftSting = leftExp.evalAndCoerceToString(env);
            String rightString = rightExp.evalAndCoerceToString(env);
            cmpResult = env.getCollator().compare(leftSting, rightString);
        } else {
            if (typeMismatchMeansNotEqual) {
                if (operator == CMP_OP_EQUALS) {
                    return false;
                } else if (operator == CMP_OP_NOT_EQUALS) {
                    return true;
                }
                // Falls through
            }
            String desc = "Can't compare values of these types. "
                    + "Allowed comparisons are between two numbers, two strings, or two dates.\n"
                    + "Left hand operand is a(n) " + ClassUtil.getFTLTypeDescription(leftValue) + ".\n"
                    + "Right hand operand is a(n) " + ClassUtil.getFTLTypeDescription(rightValue) + ".";
            if (defaultBlamed != null) {
                throw defaultBlamed.newTemplateModelException(desc);
            } else {
                throw new TemplateException(desc, env);
            }
        }

        switch (operator) {
            case CMP_OP_EQUALS: return cmpResult == 0;
            case CMP_OP_NOT_EQUALS: return cmpResult != 0;
            case CMP_OP_LESS_THAN: return cmpResult < 0;
            case CMP_OP_GREATER_THAN: return cmpResult > 0;
            case CMP_OP_LESS_THAN_EQUALS: return cmpResult <= 0;
            case CMP_OP_GREATER_THAN_EQUALS: return cmpResult >= 0;
            default: throw new RuntimeException("Unsupported comparator operator code: " + operator);
        }
    }

    private static String cmpOpToString(int operator, String operatorString) {
        if (operatorString != null) {
            return operatorString;
        } else {
            switch (operator) {
                case CMP_OP_EQUALS: return "equals";
                case CMP_OP_NOT_EQUALS: return "not-equals";
                case CMP_OP_LESS_THAN: return "less-than";
                case CMP_OP_GREATER_THAN: return "greater-than";
                case CMP_OP_LESS_THAN_EQUALS: return "less-than-equals";
                case CMP_OP_GREATER_THAN_EQUALS: return "greater-than-equals";
                default: return "???";
            }
        }
    }
    
}