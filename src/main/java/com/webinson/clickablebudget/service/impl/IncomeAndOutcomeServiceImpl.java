package com.webinson.clickablebudget.service.impl;

import com.querydsl.jpa.impl.JPAQuery;
import com.webinson.clickablebudget.assembler.VykazRadekIncomeAssembler;
import com.webinson.clickablebudget.assembler.VykazRadekOutcomeAssembler;
import com.webinson.clickablebudget.dao.*;
import com.webinson.clickablebudget.dto.IncomeAndOutcomeDto;
import com.webinson.clickablebudget.dto.VykazRadekDto;
import com.webinson.clickablebudget.entity.*;
import com.webinson.clickablebudget.service.CityService;
import com.webinson.clickablebudget.service.IncomeAndOutcomeService;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.bean.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

/**
 * Created by Slavo on 13.09.2016.
 */
@RequestScoped
@Service
public class IncomeAndOutcomeServiceImpl implements IncomeAndOutcomeService {

    @Getter
    @Setter
    HashMap<String, VykazRadekDto> mapperIncome = new HashMap();

    @Getter
    @Setter
    HashMap<String, VykazRadekDto> mapperOutcome = new HashMap();

    @Getter
    @Setter
    HashMap<String, Outcome> mapperOutcomeSave = new HashMap();

    @Autowired
    IncomeDao incomeDao;

    @Autowired
    VykazRadekIncomeAssembler vykazRadekIncomeAssembler;

    @Autowired
    VykazRadekOutcomeAssembler vykazRadekOutcomeAssembler;

    @Autowired
    CityDao cityDao;

    @Autowired
    IncomeDecreeCzechDao incomeDecreeCzechDao;

    @Autowired
    OutcomeDecreeCzechDao outcomeDecreeCzechDao;

    @Autowired
    OutcomeDao outcomeDao;

    @Autowired
    CityService cityService;

    @PersistenceContext
    EntityManager entityManager;

    @Autowired
    VykazRadekIncomeAssembler incomeAssembler;


    @Override
    public void saveIncomes(IncomeAndOutcomeDto incomeAndOutcomes) {

        List<Income> incomes = new ArrayList<Income>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (VykazRadekDto inc : incomeAndOutcomes.getIncomes()) {
            Income income = new Income();
            income.setAdjustedbudget(inc.getAdjustedBudget());
            income.setApprovedBudget(inc.getApprovedBudget());
            income.setSpentBudget(inc.getSpentBudget());
            income.setCity(cityDao.findIdByIco(incomeAndOutcomes.getVykazHlavicka().getSubjektIco()));
            income.setPolozka(inc.getPolozka());
            income.setDate(java.sql.Date.valueOf(LocalDate.parse(incomeAndOutcomes.getVykazHlavicka().getDatumVykaz(), formatter)));
            incomes.add(income);

        }

        List<Income> transform = incomes.stream()
                .collect(Collectors.groupingBy(income -> income.getPolozka()))
                .entrySet().stream()
                .map(e -> e.getValue().stream()
                        .reduce((f1, f2) -> new Income(f1.getPolozka(), f1.getApprovedBudget() + f2.getApprovedBudget(), f1.getAdjustedbudget() + f2.getAdjustedbudget(), f1.getSpentBudget() + f2.getSpentBudget(), f1.getDate(), f1.getCity())))
                .map(f -> f.get())
                .collect(Collectors.toList());

        incomeDao.save(transform);

    }

    @Override
    public void saveOutcomes(IncomeAndOutcomeDto incomeAndOutcomes) {

        List<Outcome> outcomes = new ArrayList<Outcome>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (VykazRadekDto out : incomeAndOutcomes.getOutcomes()) {
            Outcome outcome = new Outcome();
            outcome.setAdjustedbudget(out.getAdjustedBudget());
            outcome.setApprovedBudget(out.getApprovedBudget());
            outcome.setSpentBudget(out.getSpentBudget());
            outcome.setCity(cityDao.findIdByIco(incomeAndOutcomes.getVykazHlavicka().getSubjektIco()));
            outcome.setParagraf(out.getParagraf());
            outcome.setDate(java.sql.Date.valueOf(LocalDate.parse(incomeAndOutcomes.getVykazHlavicka().getDatumVykaz(), formatter)));
            outcomes.add(outcome);
        }

        List<Outcome> transform = outcomes.stream()
                .collect(Collectors.groupingBy(income -> income.getParagraf()))
                .entrySet().stream()
                .map(e -> e.getValue().stream()
                        .reduce((f1, f2) -> new Outcome(f1.getParagraf(), f1.getApprovedBudget() + f2.getApprovedBudget(), f1.getAdjustedbudget() + f2.getAdjustedbudget(), f1.getSpentBudget() + f2.getSpentBudget(), f1.getDate(), f1.getCity())))
                .map(f -> f.get())
                .collect(Collectors.toList());

        outcomeDao.save(transform);
    }

    @Override
    public List<Date> findAllDatesByCity(City city) {
        final JPAQuery<Income> query = new JPAQuery<>(entityManager);
        QIncome income = QIncome.income;
        List<Date> dates = query.from(income).select(income.date).distinct().fetch();
        return dates;
    }


    @Override
    public List<IncomeAndOutcomeDto> findAllByDate(Date date) {
        final JPAQuery<Income> query = new JPAQuery<>(entityManager);
        QIncome income = QIncome.income;
        query.from(income).select(income).where(income.date.eq(date));
        return null;
    }


    public TreeNode createIncomes(String selectedMonth, String selectedCity, String selectedYear) {

        TreeNode rootNode = new DefaultTreeNode(new VykazRadekDto("name", 1.0, 2.0, 3.0), null);

        List<VykazRadekDto> vykazRadekRootNodeList = getIncomeVykazRadekRoot(selectedYear, selectedMonth, selectedCity);

        for (VykazRadekDto vykRad : vykazRadekRootNodeList) {
            TreeNode node = new DefaultTreeNode(vykRad, rootNode);
            createSubIncomes(vykRad, node);
        }
        return rootNode;
    }

    private List<VykazRadekDto> createSubIncomes(VykazRadekDto vykaz, TreeNode node) {
        List<VykazRadekDto> vykazList = createSubVykazRadekIncomes(vykaz);
        try {
            for (VykazRadekDto vyk : vykazList) {
                TreeNode subNode = new DefaultTreeNode(vyk, node);
                createSubIncomes(vyk, subNode);
            }
            return vykazList;
        } finally {
            return vykazList;
        }
    }

    private List<VykazRadekDto> createSubVykazRadekIncomes(VykazRadekDto vykaz) {

        List<VykazRadekDto> vykazRadokNodeList = new ArrayList<>();

        if (vykaz.getChildren() != null) {
            for (VykazRadekDto vyk : vykaz.getChildren()) {
                vykazRadokNodeList.add(vyk);
            }
        }
        return vykazRadokNodeList;
    }

    public List<VykazRadekDto> getFiveIncomes(String selectedMonth, String selectedCity, String selectedYear) {

        List<VykazRadekDto> vykazy = new ArrayList<VykazRadekDto>();

        for (VykazRadekDto vykaz : vykazRadekIncomeAssembler.toDtos(incomeDao.findFiveIncomes(selectedCity, selectedMonth, selectedYear, new PageRequest(0, 5)))) {
            vykaz.setName(incomeDecreeCzechDao.findIncomeDecreeCzechByKlass(vykaz.getPolozka()).getName());
            vykazy.add(vykaz);
        }

        return vykazy;
    }

    @Override
    public List<VykazRadekDto> getFiveOutcomes(String selectedMonth, String selectedCity, String selectedYear) {

        List<VykazRadekDto> vykazy = new ArrayList<VykazRadekDto>();

        for (VykazRadekDto vykaz : vykazRadekOutcomeAssembler.toDtos(outcomeDao.findFiveOutcomes(selectedCity, selectedMonth, selectedYear, new PageRequest(0, 5)))) {
            vykaz.setName(outcomeDecreeCzechDao.findOutcomeDecreeCzechByKlass(vykaz.getParagraf()).getName());
            vykazy.add(vykaz);
        }

        return vykazy;
    }

    @Override
    public Date getLastDateByCity(String city) {
        final JPAQuery<Income> query = new JPAQuery<>(entityManager);
        QIncome income = QIncome.income;
        return query.from(income).select(income.date.max()).where(income.city.name.eq(city)).fetchFirst();
    }

    @Override
    public Date getLastDateByCityAndYear(String city, String year) {

        final JPAQuery<Income> query = new JPAQuery<>(entityManager);
        QIncome income = QIncome.income;
        return query.from(income).select(income.date.max()).where(income.city.name.eq(city).and(income.date.year().eq(Integer.parseInt(year)))).fetchFirst();

    }

    @Override
    public List<String> getAllYears(String city) {
        final JPAQuery<Income> query = new JPAQuery<>(entityManager);
        QIncome income = QIncome.income;
        List<Date> dates = query.from(income).select(income.date).where(income.city.name.eq(city)).fetch();
        List<String> dats = new ArrayList<String>();

        for (Date dat : dates) {
            String datum = new String();
            datum = dat.toString().substring(0, 4);
            dats.add(datum);
        }

        return dats;
    }

    @Override
    public VykazRadekDto getAllPrijmy(String city, String year, String month) {

        VykazRadekDto prijmy = new VykazRadekDto();
        prijmy = vykazRadekIncomeAssembler.dtosToDto(incomeDao.findGeneralIncome(city, month, year));
        return prijmy;
    }

    @Override
    public VykazRadekDto getAllVydaje(String city, String year, String month) {

        VykazRadekDto vydaje = new VykazRadekDto();
        vydaje = vykazRadekOutcomeAssembler.dtosToDto(outcomeDao.findGeneralOutcome(city, month, year));

        return vydaje;
    }

    @Override
    public VykazRadekDto getAllPrijmyForBar(String city, String year, String month) {
        List<VykazRadekDto> vykazRadekRootNodeList = getIncomeVykazRadekRoot(year, month, city);
        VykazRadekDto vykaz = new VykazRadekDto();
        vykaz.setChildren(vykazRadekRootNodeList);
        return vykaz;
    }

    @Override
    public VykazRadekDto getAllVydajeForBar(String city, String year, String month) {
        List<VykazRadekDto> vykazRadekRootNodeList = getOutcomeVykazRadekRoot(year, month, city);
        VykazRadekDto vykaz = new VykazRadekDto();
        vykaz.setChildren(vykazRadekRootNodeList);
        return vykaz;
    }

    @Override
    public VykazRadekDto createFirstRootsIncome() {

        List<VykazRadekDto> vykazy = new ArrayList<VykazRadekDto>();
        VykazRadekDto vykaz = new VykazRadekDto();
        for (String s : mapperIncome.keySet()) {
            if (s.length() == 1) {
                vykazy.add(mapperIncome.get(s));
            }
        }
        vykaz.setChildren(vykazy);
        return vykaz;
    }

    @Override
    public VykazRadekDto createFirstRootsOutcome() {

        List<VykazRadekDto> vykazy = new ArrayList<VykazRadekDto>();
        VykazRadekDto vykaz = new VykazRadekDto();
        for (String s : mapperOutcome.keySet()) {
            if (s.length() == 1) {
                vykazy.add(mapperOutcome.get(s));
            }
        }
        vykaz.setChildren(vykazy);
        return vykaz;
    }

    private List<VykazRadekDto> getIncomeVykazRadekRoot(String selectedYear, String selectedMonth, String selectedCity) {

        List<VykazRadekDto> root = new ArrayList<VykazRadekDto>();

        for (String i : incomeDao.findDistinctVykazy(selectedCity, selectedMonth, selectedYear)) {


            List<VykazRadekDto> vykazy = vykazRadekIncomeAssembler.toDtos(incomeDao.findIncomeByPolozkaString(1, String.valueOf(i), selectedCity, selectedMonth, selectedYear));

            for (VykazRadekDto vyk : vykazy) {
                mapperIncome.put(vyk.getPolozka().substring(0, 1), vykazRadekIncomeAssembler.dtosToDto(incomeDao.findIncomeByPolozkaString(1, String.valueOf(vyk.getPolozka().substring(0, 1)), selectedCity, selectedMonth, selectedYear)));
                mapperIncome.put(vyk.getPolozka().substring(0, 2), vykazRadekIncomeAssembler.dtosToDto(incomeDao.findIncomeByPolozkaString(2, String.valueOf(vyk.getPolozka().substring(0, 2)), selectedCity, selectedMonth, selectedYear)));
                mapperIncome.put(vyk.getPolozka().substring(0, 3), vykazRadekIncomeAssembler.dtosToDto(incomeDao.findIncomeByPolozkaString(3, String.valueOf(vyk.getPolozka().substring(0, 3)), selectedCity, selectedMonth, selectedYear)));
                mapperIncome.get(vyk.getPolozka().substring(0, 3)).setChildren(vykazRadekIncomeAssembler.toDtos(incomeDao.findIncomeByPolozkaString(3, String.valueOf(vyk.getPolozka().substring(0, 3)), selectedCity, selectedMonth, selectedYear)));

                for (VykazRadekDto pomVykaz : mapperIncome.get(vyk.getPolozka().substring(0, 3)).getChildren()) {
                    try {
                        pomVykaz.setName(incomeDecreeCzechDao.findIncomeDecreeCzechByKlass(pomVykaz.getPolozka()).getName());
                        mapperIncome.get(vyk.getPolozka().substring(0, 3)).setParent(pomVykaz);
                        pomVykaz.setLevelColor("fourthLevelIncome");
                    } catch (RuntimeException e) {
                    }
                }
            }

            List<VykazRadekDto> vyks2 = new ArrayList<VykazRadekDto>();
            List<VykazRadekDto> vyks3 = new ArrayList<VykazRadekDto>();
            VykazRadekDto vyk2 = new VykazRadekDto();
            VykazRadekDto vyk3 = new VykazRadekDto();
            String d = "000";

            Map<String, VykazRadekDto> map = new TreeMap<String, VykazRadekDto>(mapperIncome);

            for (String s : map.keySet()) {
                try {
                    mapperIncome.get(s).setName(incomeDecreeCzechDao.findIncomeDecreeCzechByKlass(s).getName());
                } catch (RuntimeException e) {
                }

                if (s.startsWith(i)) {

                    if (s.length() == 2) {
                        vyk2 = mapperIncome.get(s);
                        vyk2.setPolozka(s);
                        vyk2.setParent(mapperIncome.get(s.charAt(0)));
                        try {
                            vyk2.setName(incomeDecreeCzechDao.findIncomeDecreeCzechByKlass(s).getName());
                        } catch (RuntimeException e) {
                        }
                        vyks2.add(vyk2);
                        vyk2 = new VykazRadekDto();
                        mapperIncome.get(s.substring(0, 1)).setChildren(vyks2);
                    }

                    if (s.length() == 3) {

                        if (!s.substring(0, 2).equals(d.substring(0, 2))) {
                            vyks3 = new ArrayList<VykazRadekDto>();
                        }
                        vyk3 = mapperIncome.get(s);
                        vyk3.setPolozka(s);
                        try {
                            vyk3.setName(incomeDecreeCzechDao.findIncomeDecreeCzechByKlass(s).getName());
                        } catch (RuntimeException e) {
                        }
                        vyks3.add(vyk3);
                        mapperIncome.get(s.substring(0, 2)).setLevelColor("secondLevelIncome");
                        mapperIncome.get(s).setLevelColor("thirdLevelIncome");
                        mapperIncome.get(s.substring(0, 2)).setChildren(vyks3);

                        vyk3 = new VykazRadekDto();
                        d = s;
                    }

                }
            }
            root.add(mapperIncome.get(i));
        }
        return root;
    }

    private List<VykazRadekDto> getOutcomeVykazRadekRoot(String selectedYear, String selectedMonth, String selectedCity) {

        List<VykazRadekDto> root = new ArrayList<VykazRadekDto>();

        for (String o : outcomeDao.findDistinctVykazy(selectedCity, selectedMonth, selectedYear)) {

            List<VykazRadekDto> vykazy = vykazRadekOutcomeAssembler.toDtos(outcomeDao.findOutcomeByPolozkaString(1, String.valueOf(o), selectedCity, selectedMonth, selectedYear));

            for (VykazRadekDto vyk : vykazy) {
                mapperOutcome.put(vyk.getParagraf().substring(0, 1), vykazRadekOutcomeAssembler.dtosToDto(outcomeDao.findOutcomeByPolozkaString(1, String.valueOf(vyk.getParagraf().substring(0, 1)), selectedCity, selectedMonth, selectedYear)));
                mapperOutcome.put(vyk.getParagraf().substring(0, 2), vykazRadekOutcomeAssembler.dtosToDto(outcomeDao.findOutcomeByPolozkaString(2, String.valueOf(vyk.getParagraf().substring(0, 2)), selectedCity, selectedMonth, selectedYear)));
                mapperOutcome.put(vyk.getParagraf().substring(0, 3), vykazRadekOutcomeAssembler.dtosToDto(outcomeDao.findOutcomeByPolozkaString(3, String.valueOf(vyk.getParagraf().substring(0, 3)), selectedCity, selectedMonth, selectedYear)));
                mapperOutcome.get(vyk.getParagraf().substring(0, 3)).setChildren(vykazRadekOutcomeAssembler.toDtos(outcomeDao.findOutcomeByPolozkaString(3, String.valueOf(vyk.getParagraf().substring(0, 3)), selectedCity, selectedMonth, selectedYear)));

                for (VykazRadekDto pomVykaz : mapperOutcome.get(vyk.getParagraf().substring(0, 3)).getChildren()) {
                    try {
                        pomVykaz.setName(outcomeDecreeCzechDao.findOutcomeDecreeCzechByKlass(pomVykaz.getParagraf()).getName());
                        mapperOutcome.get(vyk.getParagraf().substring(0, 3)).setParent(pomVykaz);
                        pomVykaz.setLevelColor("fourthLevelOutcome");
                    } catch (RuntimeException e) {
                    }
                }
            }

            List<VykazRadekDto> vyks2 = new ArrayList<VykazRadekDto>();
            List<VykazRadekDto> vyks3 = new ArrayList<VykazRadekDto>();
            VykazRadekDto vyk2 = new VykazRadekDto();
            VykazRadekDto vyk3 = new VykazRadekDto();
            String d = "000";

            Map<String, VykazRadekDto> map = new TreeMap<String, VykazRadekDto>(mapperOutcome);

            for (String s : map.keySet()) {
                try {
                    mapperOutcome.get(s).setName(outcomeDecreeCzechDao.findOutcomeDecreeCzechByKlass(s).getName());
                } catch (RuntimeException e) {
                }

                if (s.startsWith(o)) {

                    if (s.length() == 2) {
                        vyk2 = mapperOutcome.get(s);
                        vyk2.setParagraf(s);
                        vyk2.setParent(mapperOutcome.get(s.charAt(0)));
                        try {
                            vyk2.setName(outcomeDecreeCzechDao.findOutcomeDecreeCzechByKlass(s).getName());
                        } catch (RuntimeException e) {
                        }
                        vyks2.add(vyk2);
                        vyk2 = new VykazRadekDto();
                        mapperOutcome.get(s.substring(0, 1)).setChildren(vyks2);
                    }

                    if (s.length() == 3) {

                        if (!s.substring(0, 2).equals(d.substring(0, 2))) {
                            vyks3 = new ArrayList<VykazRadekDto>();
                        }
                        vyk3 = mapperOutcome.get(s);
                        vyk3.setParagraf(s);
                        try {
                            vyk3.setName(outcomeDecreeCzechDao.findOutcomeDecreeCzechByKlass(s).getName());
                        } catch (RuntimeException e) {
                        }
                        vyks3.add(vyk3);
                        mapperOutcome.get(s.substring(0, 2)).setLevelColor("secondLevelOutcome");
                        mapperOutcome.get(s).setLevelColor("thirdLevelOutcome");

                        mapperOutcome.get(s.substring(0, 2)).setChildren(vyks3);
                        vyk3 = new VykazRadekDto();
                        d = s;
                    }

                }
            }
            root.add(mapperOutcome.get(o));
        }
        return root;
    }

    public TreeNode createOutcomes(String selectedMonth, String selectedCity, String selectedYear) {

        TreeNode rootNode = new DefaultTreeNode(new VykazRadekDto("name", 1.0, 2.0, 3.0), null);

        List<VykazRadekDto> vykazRadekRootNodeList = getOutcomeVykazRadekRoot(selectedYear, selectedMonth, selectedCity);

        for (VykazRadekDto vykRad : vykazRadekRootNodeList) {
            TreeNode node = new DefaultTreeNode(vykRad, rootNode);
            createSubOutcomes(vykRad, node);
        }
        return rootNode;
    }

    private List<VykazRadekDto> createSubOutcomes(VykazRadekDto vykaz, TreeNode node) {
        List<VykazRadekDto> vykazList = createSubVykazRadekOutcomes(vykaz);
        try {
            for (VykazRadekDto vyk : vykazList) {
                TreeNode subNode = new DefaultTreeNode(vyk, node);
                createSubOutcomes(vyk, subNode);
            }
            return vykazList;
        } finally {
            return vykazList;
        }
    }

    private List<VykazRadekDto> createSubVykazRadekOutcomes(VykazRadekDto vykaz) {

        List<VykazRadekDto> vykazRadokNodeList = new ArrayList<>();

        if (vykaz.getChildren() != null) {
            for (VykazRadekDto vyk : vykaz.getChildren()) {
                vykazRadokNodeList.add(vyk);
            }
        }
        return vykazRadokNodeList;
    }

}
