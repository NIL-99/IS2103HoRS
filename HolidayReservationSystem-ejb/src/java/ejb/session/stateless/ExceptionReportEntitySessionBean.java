/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ejb.session.stateless;

import entity.ExceptionReportEntity;
import java.time.LocalDateTime;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import util.enumeration.ExceptionReportTypeEnum;
import util.exception.ExceptionReportNotFoundException;
import util.exception.UnknownPersistenceException;

/**
 *
 * @author mingy
 */
@Stateless
public class ExceptionReportEntitySessionBean implements ExceptionReportEntitySessionBeanRemote, ExceptionReportEntitySessionBeanLocal {

    @PersistenceContext(unitName = "HolidayReservationSystem-ejbPU")
    private EntityManager em;

    private final ValidatorFactory validatorFactory;
    private final Validator validator;

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
    public ExceptionReportEntitySessionBean() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Override
    public Long createNewExceptionReport(ExceptionReportEntity newExceptionReport) throws UnknownPersistenceException {
        try {
            em.persist(newExceptionReport);
            em.flush();

            return newExceptionReport.getExceptionReportId();
        } catch (PersistenceException ex) {
            throw new UnknownPersistenceException(ex.getMessage());
        }
    }

    @Override
    public List<ExceptionReportEntity> retrieveAllExceptionReport() {
        Query query = em.createQuery("SELECT er FROM ExceptionReportEntity er");
        List<ExceptionReportEntity> listOfExceptionReportEntities = query.getResultList();
        for (ExceptionReportEntity exceptionReportEntity : listOfExceptionReportEntities) {
            exceptionReportEntity.getReservationEntity().getRoomEntity();
        }
        return listOfExceptionReportEntities;
    }

    @Override
    public ExceptionReportEntity retrieveExceptionReportById(Long exceptionReportId) throws ExceptionReportNotFoundException {
        ExceptionReportEntity exceptionReport = em.find(ExceptionReportEntity.class, exceptionReportId);

        if (exceptionReport != null) {
            exceptionReport.getReservationEntity();

            return exceptionReport;
        } else {
            throw new ExceptionReportNotFoundException("Exception Report ID " + exceptionReportId + " does not exist");
        }
    }
    
    public List<ExceptionReportEntity> retrieveExceptionReportsByTypeAndDate(ExceptionReportTypeEnum exceptionReportTypeEnum, LocalDateTime date) {
        List<ExceptionReportEntity> listOfExceptionReportEntities = em.createQuery("SELECT er FROM ExceptionReportEntity er WHERE er.exceptionReportTypeEnum = :inExceptionReportType AND  er.generationDate = :inDate ")
                .setParameter("exceptionReportTypeEnum", exceptionReportTypeEnum)
                .setParameter("generationDate", date)
                .getResultList();
        for(ExceptionReportEntity exceptionReportEntity : listOfExceptionReportEntities) {
            exceptionReportEntity.getReservationEntity().getRoomEntity().getRoomTypeEntity();
        }
        return listOfExceptionReportEntities;
    }

    @Override
    public void deleteExceptionReport(Long exceptionReportId) throws ExceptionReportNotFoundException {
        ExceptionReportEntity exceptionReport = em.find(ExceptionReportEntity.class, exceptionReportId);

        if (exceptionReport != null) {
            em.remove(exceptionReport);
        } else {
            throw new ExceptionReportNotFoundException("Exception Report IDD " + exceptionReportId + " does not exist");
        }
    }

    @Override
    public void updateExceptionReport(Long oldExceptionReportId, ExceptionReportEntity newExceptionReport) throws ExceptionReportNotFoundException, UnknownPersistenceException {
        try {
            ExceptionReportEntity oldExceptionReport = retrieveExceptionReportById(oldExceptionReportId);
            Long newExceptionReportId = createNewExceptionReport(newExceptionReport);

            newExceptionReport.setReservationEntity(oldExceptionReport.getReservationEntity());
            em.remove(oldExceptionReport);
        } catch (PersistenceException ex) {
            throw new UnknownPersistenceException(ex.getMessage());
        } catch (ExceptionReportNotFoundException ex) {
            throw new ExceptionReportNotFoundException("Exception Report ID " + oldExceptionReportId + " does not exist");
        }
    }
}
