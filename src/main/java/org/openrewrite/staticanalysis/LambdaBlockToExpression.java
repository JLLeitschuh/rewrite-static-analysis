/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.staticanalysis;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.SearchResult;

import java.util.List;

import static org.openrewrite.Tree.randomId;

public class LambdaBlockToExpression extends Recipe {
    @Override
    public String getDisplayName() {
        return "Simplify lambda blocks to expressions";
    }

    @Override
    public String getDescription() {
        return "Single-line statement lambdas returning a value can be replaced with expression lambdas.";
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        // Make this applicable only to Java source files. This transformation doesn't apply to Groovy
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext executionContext) {
                if(cu instanceof J.CompilationUnit) {
                    return cu.withMarkers(cu.getMarkers().add(new SearchResult(randomId(), null)));
                }
                return cu;
            }
        };
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Lambda visitLambda(J.Lambda lambda, ExecutionContext executionContext) {
                J.Lambda l = super.visitLambda(lambda, executionContext);
                if (lambda.getBody() instanceof J.Block) {
                    List<Statement> statements = ((J.Block) lambda.getBody()).getStatements();
                    if (statements.size() == 1 && statements.get(0) instanceof J.Return) {
                        return l.withBody(((J.Return) statements.get(0)).getExpression());
                    }
                }
                return l;
            }
        };
    }
}
