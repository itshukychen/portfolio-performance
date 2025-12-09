package name.abuchen.portfolio.ui.views.taxonomy;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

public class RecalculateTargetsAttachedModel implements TaxonomyModel.AttachedModel
{
    @Override
    public void recalculate(TaxonomyModel model)
    {
        TaxonomyNode virtualRootNode = model.getVirtualRootNode();
        TaxonomyNode unassignedNode = model.getUnassignedNode();

        virtualRootNode.setTarget(virtualRootNode.getActual().subtract(unassignedNode.getActual()));

        // First pass: calculate targets for classification nodes
        model.visitAll(node -> {
            if (node.isClassification() && !node.isRoot())
            {
                Money parent = node.getParent().getTarget();
                Money parentAssignmentDirectChilds = node.getParent().getChildren().stream()
                                .filter(child -> child.isAssignment())
                                .map(assignment -> assignment.getActual())
                                .reduce(Money.of(parent.getCurrencyCode(), 0), Money::add);
                Money target = Money.of(parent.getCurrencyCode(),
                                Math.round(parent.subtract(parentAssignmentDirectChilds).getAmount() * node.getWeight()
                                                / (double) Classification.ONE_HUNDRED_PERCENT));
                node.setTarget(target);
            }
        });

        // Second pass: calculate targets for assignment nodes based on parent classification's target
        // Weights are relative to each other, so we need to sum all assignment weights for each parent
        // and distribute the parent's target proportionally
        model.visitAll(parentNode -> {
            if (parentNode.isClassification() && parentNode.getTarget() != null)
            {
                // Get all assignment children for this parent
                java.util.List<TaxonomyNode> assignmentChildren = parentNode.getChildren().stream()
                                .filter(child -> child.isAssignment())
                                .collect(java.util.stream.Collectors.toList());
                
                if (!assignmentChildren.isEmpty())
                {
                    // Sum all assignment weights
                    long totalWeight = assignmentChildren.stream()
                                    .mapToLong(TaxonomyNode::getWeight)
                                    .sum();
                    
                    if (totalWeight > 0)
                    {
                        Money parentTarget = parentNode.getTarget();
                        // Distribute parent target proportionally based on relative weights
                        for (TaxonomyNode assignmentNode : assignmentChildren)
                        {
                            long assignmentWeight = assignmentNode.getWeight();
                            Money target = Money.of(parentTarget.getCurrencyCode(),
                                            Math.round(parentTarget.getAmount() * assignmentWeight
                                                            / (double) totalWeight));
                            assignmentNode.setTarget(target);
                        }
                    }
                }
            }
        });
    }
}
