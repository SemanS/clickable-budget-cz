package com.webinson.clickablebudget.bean;


import com.ocpsoft.pretty.PrettyContext;
import com.webinson.clickablebudget.dao.IncomeDao;
import com.webinson.clickablebudget.dto.VykazRadekDto;
import com.webinson.clickablebudget.service.CityService;
import com.webinson.clickablebudget.service.IncomeAndOutcomeService;
import com.webinson.clickablebudget.utils.MonthFormatter;
import com.webinson.clickablebudget.utils.MonthFormatterReverse;
import lombok.Getter;
import lombok.Setter;
import org.chartistjsf.model.chart.*;
import org.primefaces.event.ItemSelectEvent;
import org.primefaces.model.TreeNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.SystemEvent;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Slavo on 13.09.2016.
 */
@Component
@ViewScoped
public class ClickableBudgetView implements Serializable {

    @Getter
    @Setter
    TreeNode selectedRadek;

    @Getter
    @Setter
    VykazRadekDto generalIncome;

    @Getter
    @Setter
    VykazRadekDto generalOutcome;

    @Getter
    @Setter
    VykazRadekDto generalDifference;

    @Getter
    @Setter
    private PieChartModel pieChartModelIncome;

    @Getter
    @Setter
    private PieChartModel pieChartModelOutcome;

    @Getter
    @Setter
    List<VykazRadekDto> fiveIncomes;

    @Getter
    @Setter
    List<VykazRadekDto> fiveOutcomes;

    @Getter
    @Setter
    private BarChartModel barChartModelIncome;

    @Getter
    @Setter
    private BarChartModel barChartModelOutcome;

    @Getter
    @Setter
    VykazRadekDto selectedVykazIncome;

    @Getter
    @Setter
    VykazRadekDto selectedVykazOutcome;

    @Autowired
    @Setter
    private IncomeAndOutcomeService incomeAndOutcomeService;

    @Autowired
    private CityService cityService;

    @Autowired
    private IncomeDao incomeDao;

    @Getter
    @Setter
    private String selectedMonth;

    @Getter
    @Setter
    public String selectedYear;

    @Getter
    @Setter
    @ManagedProperty(value = "#{param.selectedCity}")
    private String selectedCity;

    @Getter
    @Setter
    private TreeNode rootIncomes;

    @Getter
    @Setter
    private TreeNode rootOutcomes;

    @Getter
    @Setter
    private VykazRadekDto selectedVykazRadekIncome;

    @Getter
    @Setter
    private VykazRadekDto selectedVykazRadekOutcome;

    @Getter
    @Setter
    private TreeNode selectedNode;

    public List<String> getMonths(String thisYear) {

        MonthFormatter monthFormatter = new MonthFormatter();
        List<String> months = new ArrayList<String>();

        for (String dat : incomeDao.findAllMonthsByYear(selectedCity, selectedYear)) {
            months.add(monthFormatter.monthFormat(dat));
        }
        return months;
    }

    public List<String> getYears() {

        List<String> years = new ArrayList<String>();

        for (String dat : incomeDao.findAllYearsByCity(selectedCity)) {
            years.add(dat);
        }
        return years;
    }

    public String onYearChange(String year) {

        selectedMonth = incomeAndOutcomeService.getLastDateByCityAndYear(selectedCity, year).toString().substring(5, 7);
        rootIncomes = null;
        rootIncomes = incomeAndOutcomeService.createIncomes(selectedMonth, selectedCity, year);
        rootOutcomes = incomeAndOutcomeService.createOutcomes(selectedMonth, selectedCity, year);
        selectedYear = year;
        return year;
    }

    public String onMonthChange(String month, String year) {

        if (year == "") {
            year = selectedYear;
        }

        MonthFormatterReverse monthFormatterReverse = new MonthFormatterReverse();
        selectedMonth = monthFormatterReverse.monthFormat(month);
        selectedCity = PrettyContext.getCurrentInstance().getRequestURL().toURL().substring(6);
        rootIncomes = null;
        rootOutcomes = null;
        rootIncomes = incomeAndOutcomeService.createIncomes(selectedMonth, selectedCity, year);
        rootOutcomes = incomeAndOutcomeService.createOutcomes(selectedMonth, selectedCity, year);
        generalIncome = incomeAndOutcomeService.getAllPrijmy(selectedCity, year, selectedMonth);
        generalOutcome = incomeAndOutcomeService.getAllVydaje(selectedCity, year, selectedMonth);

        barChartModelIncome = createBarModelExpandIncome(incomeAndOutcomeService.getAllPrijmyForBar(selectedCity, year, selectedMonth));
        barChartModelOutcome = createBarModelExpandOutcome(incomeAndOutcomeService.getAllVydajeForBar(selectedCity, year, selectedMonth));

        createPieChartIncome();
        createPieChartOutcome();
        return month;
    }


    @PostConstruct
    public void init() {

        selectedCity = PrettyContext.getCurrentInstance().getRequestURL().toURL().substring(6);
        System.out.println(PrettyContext.getCurrentInstance().getRequestURL().toURL().substring(6));
        selectedYear = incomeAndOutcomeService.getLastDateByCity(selectedCity).toString().substring(0, 4);
        fiveIncomes = incomeAndOutcomeService.getFiveIncomes(incomeAndOutcomeService.getLastDateByCity(selectedCity).toString().substring(5, 7), selectedCity, selectedYear);
        fiveOutcomes = incomeAndOutcomeService.getFiveOutcomes(incomeAndOutcomeService.getLastDateByCity(selectedCity).toString().substring(5, 7), selectedCity, selectedYear);

        rootIncomes = incomeAndOutcomeService.createIncomes(incomeAndOutcomeService.getLastDateByCity(selectedCity).toString().substring(5, 7), selectedCity, selectedYear);
        rootOutcomes = incomeAndOutcomeService.createOutcomes(incomeAndOutcomeService.getLastDateByCity(selectedCity).toString().substring(5, 7), selectedCity, selectedYear);

        selectedVykazIncome = incomeAndOutcomeService.createFirstRootsIncome();
        selectedVykazOutcome = incomeAndOutcomeService.createFirstRootsOutcome();

        barChartModelIncome = createBarModelExpandIncome(selectedVykazIncome);
        barChartModelOutcome = createBarModelExpandOutcome(selectedVykazOutcome);

        generalIncome = incomeAndOutcomeService.getAllPrijmy(selectedCity, selectedYear, incomeAndOutcomeService.getLastDateByCity(selectedCity).toString().substring(5, 7));
        generalOutcome = incomeAndOutcomeService.getAllVydaje(selectedCity, selectedYear, incomeAndOutcomeService.getLastDateByCity(selectedCity).toString().substring(5, 7));
        generalDifference = difference(selectedCity, selectedYear, selectedMonth, generalIncome, generalOutcome);
        createPieChartIncome();
        createPieChartOutcome();
        selectedMonth = incomeAndOutcomeService.getLastDateByCity(selectedCity).toString().substring(5, 7);
    }

    public VykazRadekDto difference(String selectedCity, String selectedYear, String selectedMonth, VykazRadekDto generalIncome, VykazRadekDto generalOutcome) {

        VykazRadekDto newDifference = new VykazRadekDto();
        newDifference.setApprovedBudget(generalIncome.getApprovedBudget() - generalOutcome.getApprovedBudget());
        newDifference.setAdjustedBudget(generalIncome.getAdjustedBudget() - generalOutcome.getAdjustedBudget());
        newDifference.setSpentBudget(generalIncome.getSpentBudget() - generalOutcome.getSpentBudget());
        //newDifference.setStateInt((generalIncome.getStateInt() + generalOutcome.getStateInt()) / 2);
        newDifference.setStateInt2(String.valueOf((generalIncome.getStateInt() + generalOutcome.getStateInt()) / 2) + "%");
        return newDifference;
    }

    public void onNodeExpandIncome(VykazRadekDto vykaz) {
        createBarModelExpandIncome(vykaz);
    }

    public void onNodeCollapseIncome(VykazRadekDto vykaz) {
        vykaz = selectedVykazIncome;
        createBarModelExpandIncome(vykaz);
    }

    public void onNodeExpandOutcome(VykazRadekDto vykaz) {
        createBarModelExpandOutcome(vykaz);
    }

    public void onNodeCollapseOutcome(VykazRadekDto vykaz) {
        vykaz = selectedVykazOutcome;
        createBarModelExpandOutcome(vykaz);
    }

    public BarChartModel createBarModelExpandIncome(VykazRadekDto selectedVykaz) {
        //Random random = new Random();
        barChartModelIncome = new BarChartModel();
        barChartModelIncome.setAspectRatio(AspectRatio.DOUBLE_OCTAVE);

        BarChartSeries series1 = new BarChartSeries();
        BarChartSeries series2 = new BarChartSeries();
        series1.setName("Prijate");
        series2.setName("Prijate");

        for (VykazRadekDto vyk : selectedVykaz.getChildren()) {
            barChartModelIncome.addLabel(vyk.getName());
            series1.set(vyk.getSpentBudget());
            series2.set(vyk.getAdjustedBudget());
        }

        barChartModelIncome.addSeries(series1);
        barChartModelIncome.addSeries(series2);

        Axis xAxis = barChartModelIncome.getAxis(AxisType.X);
        xAxis.setShowGrid(false);
        xAxis.setAxisPosition(AxisPosition.START);

        barChartModelIncome.getAxis(AxisType.Y).setYLabelOffset(20);
        barChartModelIncome.getAxis(AxisType.Y).setScaleMinSpace(40);

        Axis yAxis = barChartModelIncome.getAxis(AxisType.Y);
        yAxis.setShowGrid(true);
        yAxis.setYLabelOffset(2);
        yAxis.setAxisPosition(AxisPosition.START);
        yAxis.setShowLabel(false);

        //barChartModelIncome.setShowTooltip(true);
        barChartModelIncome.setSeriesBarDistance(12);
        barChartModelIncome.setStackBars(true);
        return barChartModelIncome;
    }

    public BarChartModel createBarModelCollapseIncome(VykazRadekDto selectedVykaz) {

        barChartModelIncome = new BarChartModel();
        barChartModelIncome.setAspectRatio(AspectRatio.DOUBLE_OCTAVE);

        BarChartSeries series1 = new BarChartSeries();
        BarChartSeries series2 = new BarChartSeries();
        series1.setName("Prijate");
        series2.setName("Prijate");

        for (VykazRadekDto vyk : selectedVykaz.getParent().getChildren()) {
            barChartModelIncome.addLabel(vyk.getName());
            series1.set(vyk.getSpentBudget());
            series2.set(vyk.getAdjustedBudget());
        }

        barChartModelIncome.addSeries(series1);
        barChartModelIncome.addSeries(series2);

        Axis xAxis = barChartModelIncome.getAxis(AxisType.X);
        xAxis.setShowGrid(false);

        barChartModelIncome.setShowTooltip(true);
        barChartModelIncome.setSeriesBarDistance(12);
        barChartModelIncome.setStackBars(true);

        return barChartModelIncome;
    }

    public BarChartModel createBarModelExpandOutcome(VykazRadekDto selectedVykaz) {
        //Random random = new Random();
        barChartModelOutcome = new BarChartModel();
        barChartModelOutcome.setAspectRatio(AspectRatio.DOUBLE_OCTAVE);

        BarChartSeries series1 = new BarChartSeries();
        BarChartSeries series2 = new BarChartSeries();
        series1.setName("Prijate");
        series2.setName("Prijate");

        for (VykazRadekDto vyk : selectedVykaz.getChildren()) {
            barChartModelOutcome.addLabel(vyk.getName());
            series1.set(vyk.getSpentBudget());
            series2.set(vyk.getAdjustedBudget());
        }

        barChartModelOutcome.addSeries(series1);
        barChartModelOutcome.addSeries(series2);

        Axis xAxis = barChartModelOutcome.getAxis(AxisType.X);
        xAxis.setShowGrid(false);
        xAxis.setAxisPosition(AxisPosition.START);

        barChartModelOutcome.getAxis(AxisType.Y).setYLabelOffset(10);
        barChartModelOutcome.getAxis(AxisType.Y).setScaleMinSpace(20);

        Axis yAxis = barChartModelOutcome.getAxis(AxisType.Y);
        yAxis.setShowGrid(true);
        yAxis.setYLabelOffset(2);
        yAxis.setAxisPosition(AxisPosition.START);
        yAxis.setShowLabel(false);
        //System.out.println(barChartModelIncome.getSeries().get(0).getData().toString());

        //barChartModelIncome.setShowTooltip(true);
        barChartModelOutcome.setSeriesBarDistance(12);
        barChartModelOutcome.setStackBars(true);
        //barChartModelIncome.setAnimateAdvanced(true);
        return barChartModelOutcome;
    }

    public BarChartModel createBarModelCollapseOutcome(VykazRadekDto selectedVykaz) {

        barChartModelOutcome = new BarChartModel();
        barChartModelOutcome.setAspectRatio(AspectRatio.DOUBLE_OCTAVE);

        BarChartSeries series1 = new BarChartSeries();
        BarChartSeries series2 = new BarChartSeries();
        series1.setName("Prijate");
        series2.setName("Prijate");

        for (VykazRadekDto vyk : selectedVykaz.getParent().getChildren()) {
            barChartModelOutcome.addLabel(vyk.getName());
            series1.set(vyk.getSpentBudget());
            series2.set(vyk.getAdjustedBudget());
        }

        barChartModelOutcome.addSeries(series1);
        barChartModelOutcome.addSeries(series2);

        Axis xAxis = barChartModelOutcome.getAxis(AxisType.X);
        xAxis.setShowGrid(false);

        barChartModelOutcome.setShowTooltip(true);
        barChartModelOutcome.setSeriesBarDistance(12);
        barChartModelOutcome.setStackBars(true);

        return barChartModelOutcome;
    }

    public void createPieChartOutcome() {
        pieChartModelOutcome = new PieChartModel();

        pieChartModelOutcome.addLabel("Vyčerpáno");
        pieChartModelOutcome.addLabel("Vyčerpáme");

        pieChartModelOutcome.set(generalOutcome.getAdjustedBudget());
        pieChartModelOutcome.set(generalOutcome.getAdjustedBudget() - generalOutcome.getSpentBudget());

        //pieChartModel.setShowTooltip(true);
    }

    public void createPieChartIncome() {
        pieChartModelIncome = new PieChartModel();

        pieChartModelIncome.addLabel("Přijde");
        pieChartModelIncome.addLabel("Přišlo");

        pieChartModelIncome.set(generalIncome.getAdjustedBudget());
        pieChartModelIncome.set(generalIncome.getAdjustedBudget() - generalIncome.getSpentBudget());

        //pieChartModel.setShowTooltip(true);
    }

    public void pieItemSelectIncome(ItemSelectEvent event) {
        FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Item selected", "Item Value: "
                + pieChartModelIncome.getData().get(event.getItemIndex()));

        FacesContext.getCurrentInstance().addMessage(event.getComponent().getClientId(), msg);
    }

    public void pieItemSelectOutcome(ItemSelectEvent event) {
        FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Item selected", "Item Value: "
                + pieChartModelOutcome.getData().get(event.getItemIndex()));

        FacesContext.getCurrentInstance().addMessage(event.getComponent().getClientId(), msg);
    }

    @Getter
    @Setter
    private boolean incAndOutRen = true;

    @Getter
    @Setter
    private boolean incRen = true;

    @Getter
    @Setter
    private boolean outRen = true;

    public boolean renderIncomesAndOutcomes() {
        incAndOutRen = !incAndOutRen;

        System.out.println("finding..... (" + incAndOutRen + ")" + this);
        return incAndOutRen;
    }

    public void renderIncomes() {
        incRen = !incRen;
        System.out.println("finding..... (" + incRen + ")" + this);
    }

    public void renderOutcomes() {
        outRen = !outRen;
        System.out.println("finding..... (" + outRen + ")" + this);
    }

}
