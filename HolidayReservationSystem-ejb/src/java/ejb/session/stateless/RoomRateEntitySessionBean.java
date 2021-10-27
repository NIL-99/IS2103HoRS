/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ejb.session.stateless;

import entity.RoomRateEntity;
import entity.RoomTypeEntity;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import util.exception.RoomRateNotFoundException;
import util.exception.UnknownPersistenceException;

/**
 *
 * @author mingy
 */
@Stateless
public class RoomRateEntitySessionBean implements RoomRateEntitySessionBeanRemote, RoomRateEntitySessionBeanLocal {

    @EJB
    private RoomTypeEntitySessionBeanLocal roomTypeEntitySessionBeanLocal;

    @PersistenceContext(unitName = "HolidayReservationSystem-ejbPU")
    private EntityManager em;

    private final ValidatorFactory validatorFactory;
    private final Validator validator;

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
    public RoomRateEntitySessionBean() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Override
    public Long createNewRoomRate(RoomRateEntity newRoomRate) throws UnknownPersistenceException {

        try {
            em.persist(newRoomRate);
            em.flush();

            return newRoomRate.getRoomRateId();
        } catch (PersistenceException ex) {
            throw new UnknownPersistenceException(ex.getMessage());
        }
    }

    @Override
    public List<RoomRateEntity> retrieveAllRoomRate() {
        Query query = em.createQuery("SELECT rr FROM RoomRataeEntity rr");

        return query.getResultList();
    }

    @Override
    public RoomRateEntity retrieveRoomRateById(Long roomRateId) throws RoomRateNotFoundException {
        RoomRateEntity roomRate = em.find(RoomRateEntity.class, roomRateId);

        if (roomRate != null) {
            roomRate.getRoomTypeEntity();
            return roomRate;
        } else {
            throw new RoomRateNotFoundException("Room Rate ID " + roomRateId + " does not exist");
        }
    }

    @Override
    public void deleteRoomRate(Long roomRateId) throws RoomRateNotFoundException {

        RoomRateEntity roomRate = em.find(RoomRateEntity.class, roomRateId);

        if (roomRate != null) {
            em.remove(roomRate);
        } else {
            throw new RoomRateNotFoundException("Room Rate ID " + roomRateId + " does not exist");
        }

    }

    //ISSUE HERE, MIGHT NOT BE ABLE TO UPDATE PROPERLY
    @Override
    public void updateRoomRate(Long oldRoomRateId, RoomRateEntity newRoomRate) throws UnknownPersistenceException {
        try {
            RoomRateEntity oldRoomRate = em.find(RoomRateEntity.class, oldRoomRateId);
            Long newRoomRateId = createNewRoomRate(newRoomRate);

            em.remove(oldRoomRate);
        } catch (PersistenceException ex) {
            throw new UnknownPersistenceException(ex.getMessage());
        }
    }
}
