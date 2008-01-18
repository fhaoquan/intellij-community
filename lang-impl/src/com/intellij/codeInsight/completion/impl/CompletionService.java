/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.codeInsight.completion.impl;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.patterns.MatchingContext;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.TraverseContext;
import com.intellij.util.PrioritizedQueryExecutor;
import com.intellij.util.PrioritizedQueryFactory;
import com.intellij.util.QueryResultSet;

/**
 * @author peter
 */
public class CompletionService {
  private final PrioritizedQueryFactory<LookupElement, CompletionParameters> myBasicCompletionQueryFactory = new PrioritizedQueryFactory<LookupElement, CompletionParameters>();
  private final PrioritizedQueryFactory<LookupElement, CompletionParameters> mySmartCompletionQueryFactory = new PrioritizedQueryFactory<LookupElement, CompletionParameters>();
  private final PrioritizedQueryFactory<LookupElement, CompletionParameters> myClassNameCompletionQueryFactory = new PrioritizedQueryFactory<LookupElement, CompletionParameters>();

  public CompletionService() {
    for (final CompletionContributor contributor : Extensions.getExtensions(CompletionContributor.EP_NAME)) {
      contributor.registerCompletionProviders(new CompletionRegistrar() {
        public CompletionPlace<LookupElement, CompletionParameters> extendBasicCompletion(final ElementPattern place) {
          return new CompletionPlaceImpl<LookupElement, CompletionParameters>(place, myBasicCompletionQueryFactory);
        }

        public CompletionPlace<LookupElement, CompletionParameters> extendSmartCompletion(final ElementPattern place) {
          return new CompletionPlaceImpl<LookupElement, CompletionParameters>(place, mySmartCompletionQueryFactory);
        }

        public CompletionPlace<LookupElement, CompletionParameters> extendClassNameCompletion(final ElementPattern place) {
          return new CompletionPlaceImpl<LookupElement, CompletionParameters>(place, myClassNameCompletionQueryFactory);
        }
      });
    }
  }

  public PrioritizedQueryFactory<LookupElement, CompletionParameters> getBasicCompletionQueryFactory() {
    return myBasicCompletionQueryFactory;
  }

  public PrioritizedQueryFactory<LookupElement, CompletionParameters> getSmartCompletionQueryFactory() {
    return mySmartCompletionQueryFactory;
  }

  public PrioritizedQueryFactory<LookupElement, CompletionParameters> getClassNameCompletionQueryFactory() {
    return myClassNameCompletionQueryFactory;
  }

  public static CompletionService getCompletionService() {
    return ServiceManager.getService(CompletionService.class);
  }

  public static class CompletionPlaceImpl<Result, Params extends CompletionParameters> implements CompletionPlace<Result,Params> {
    private final ElementPattern myPlace;
    private final PrioritizedQueryFactory<Result, Params> myQueryFactory;
    private double myPriority;

    protected CompletionPlaceImpl(final ElementPattern place, final PrioritizedQueryFactory<Result, Params> queryFactory) {
      myPlace = place;
      myQueryFactory = queryFactory;
    }

    public CompletionPlace<Result, Params> onPriority(final double priority) {
      myPriority = priority;
      return this;
    }

    public void withProvider(final CompletionProvider<Result, Params> provider) {
      myQueryFactory.registerExecutor(myPriority, new PrioritizedQueryExecutor<Result, Params>() {
        public void execute(final Params parameters, final QueryResultSet<Result> resultSet) {
          final MatchingContext matchingContext = new MatchingContext();
          if (myPlace.accepts(parameters.getPosition(), matchingContext, new TraverseContext())) {
            provider.addCompletions(parameters, matchingContext, resultSet);
          }
        }
      });
    }


  }
}
