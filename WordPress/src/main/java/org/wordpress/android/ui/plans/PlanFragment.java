package org.wordpress.android.ui.plans;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.plans.models.Feature;
import org.wordpress.android.ui.plans.models.Plan;
import org.wordpress.android.ui.plans.models.PlanFeaturesHighlightSection;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import javax.inject.Inject;

public class PlanFragment extends Fragment {
    private static final String SITE_PLAN = "SITE_PLAN";

    private ViewGroup mPlanContainerView;
    private Plan mPlanDetails;

    @Inject protected ImageManager mImageManager;

    public static PlanFragment newInstance(Plan sitePlan) {
        PlanFragment fragment = new PlanFragment();
        fragment.setSitePlan(sitePlan);
        AppLog.d(AppLog.T.PLANS, "PlanFragment newInstance");
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SITE_PLAN)) {
                Serializable serial = savedInstanceState.getSerializable(SITE_PLAN);
                if (serial instanceof Plan) {
                    setSitePlan((Plan) serial);
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.plan_fragment, container, false);
        mPlanContainerView = (LinearLayout) rootView.findViewById(R.id.plan_container);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((WordPress) getActivity().getApplicationContext()).component().inject(this);
        showPlans();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(SITE_PLAN, mPlanDetails);
        super.onSaveInstanceState(outState);
    }

    private void showPlans() {
        if (!isAdded()) {
            return;
        }

        if (mPlanDetails == null) {
            // TODO This should never happen - Fix this. Close the activity?
            AppLog.w(AppLog.T.PLANS, "empty plan data in fragment");
            return;
        }

        int iconSize = getActivity().getResources().getDimensionPixelSize(R.dimen.plan_icon_size);
        ImageView imgPlan = getView().findViewById(R.id.image_plan_icon);
        String iconUrl = PlansUtils.getIconUrlForPlan(mPlanDetails, iconSize);
        if (!TextUtils.isEmpty(iconUrl)) {
            mImageManager.load(imgPlan, ImageType.PLAN, iconUrl);
            imgPlan.setVisibility(View.VISIBLE);
        } else {
            imgPlan.setVisibility(View.GONE);
        }

        // show product short name in bold, ex: "WordPress.com <b>Premium</b>"
        TextView txtProductName = (TextView) getView().findViewById(R.id.text_product_name);
        String productShortName = mPlanDetails.getProductNameShort();
        String productName = mPlanDetails.getProductName().replace(productShortName,
                                                                   "<b>" + productShortName + "</b>");
        txtProductName.setText(Html.fromHtml(productName));

        TextView txtTagLine = (TextView) getView().findViewById(R.id.text_tagline);
        txtTagLine.setText(StringEscapeUtils.unescapeHtml4(mPlanDetails.getTagline()));

        addFeaturesToHighlight();
    }

    private void addFeaturesToHighlight() {
        HashMap<String, Feature> globalFeatures = PlansUtils.getFeatures();
        if (globalFeatures == null) {
            AppLog.w(AppLog.T.PLANS, "no global features");
            return;
        }

        ArrayList<PlanFeaturesHighlightSection> sectionsToHighlight = mPlanDetails.getFeaturesHighlightSections();
        if (sectionsToHighlight == null) {
            AppLog.w(AppLog.T.PLANS, "no sections to highlight");
            return;
        }

        for (PlanFeaturesHighlightSection section : sectionsToHighlight) {
            // add section title if it's not empty
            addSectionTitle(section.getTitle());
            // add features to highlight in this section
            ArrayList<String> featuresToHighlight = section.getFeatures();
            for (String featureSlug : featuresToHighlight) {
                addFeature(globalFeatures.get(featureSlug));
            }
        }
    }

    private void addSectionTitle(String title) {
        if (TextUtils.isEmpty(title)) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.plan_section_title, mPlanContainerView, false);

        TextView txtTitle = (TextView) view.findViewById(R.id.text_section_title);
        txtTitle.setText(StringEscapeUtils.unescapeHtml4(title));

        mPlanContainerView.addView(view);
    }

    private void addFeature(Feature feature) {
        if (feature == null) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.plan_feature_item, mPlanContainerView, false);

        TextView txtTitle = (TextView) view.findViewById(R.id.text_feature_title);
        TextView txtDescription = (TextView) view.findViewById(R.id.text_feature_description);
        String title = StringEscapeUtils.unescapeHtml4(feature.getTitleForPlan(mPlanDetails.getProductID()));
        String description =
                StringEscapeUtils.unescapeHtml4(feature.getDescriptionForPlan(mPlanDetails.getProductID()));
        txtTitle.setText(title);
        txtDescription.setText(description);
        mPlanContainerView.addView(view);

        // TODO: right now icon is always empty, so we show noticon_publish as a placeholder
        ImageView imgIcon = view.findViewById(R.id.image_icon);
        String iconUrl = feature.getIconForPlan(mPlanDetails.getProductID());
        mImageManager.load(imgIcon, ImageType.PLAN, iconUrl);
    }

    private void setSitePlan(@NonNull Plan sitePlan) {
        mPlanDetails = sitePlan;
    }
}
