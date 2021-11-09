/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ejb.session.stateless;

import entity.GuestEntity;
import entity.ReservationEntity;
import entity.RoomEntity;
import entity.RoomRateEntity;
import entity.RoomTypeEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import util.enumeration.RoomRateTypeEnum;
import util.enumeration.RoomStatusEnum;
import util.exception.CreateNewReservationException;
import util.exception.InputDataValidationException;
import util.exception.InsufficientRoomsAvailableException;
import util.exception.ReservationNotFoundException;
import util.exception.RoomRateNotFoundException;
import util.exception.UnknownPersistenceException;
import util.exception.UpdateReservationException;

/**
 *
 * @author mingy
 */
@Stateless
public class ReservationEntitySessionBean implements ReservationEntitySessionBeanRemote, ReservationEntitySessionBeanLocal {

    @EJB
    private RoomRateEntitySessionBeanLocal roomRateEntitySessionBeanLocal;
    @Resource
    private EJBContext eJBContext;

    @PersistenceContext(unitName = "HolidayReservationSystem-ejbPU")
    private EntityManager em;

    private final ValidatorFactory validatorFactory;
    private final Validator validator;

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
    public ReservationEntitySessionBean() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Override
    public Long createNewReservation(ReservationEntity newReservation, List<String> listOfRoomRateNames) throws CreateNewReservationException, UnknownPersistenceException, InputDataValidationException {
        Set<ConstraintViolation<ReservationEntity>> constraintViolations = validator.validate(newReservation);
        //CHECK INVENTORY FOR THAT ROOMTYPE ONCE MORE BEFORE PERSISTING RESERVATION
        Query query = em.createQuery("SELECT r FROM RoomEntity r WHERE r.roomStatusEnum = :inRoomStatus AND r.roomTypeEntity.roomTypeName = :inRoomTypeEntity").setParameter("inRoomStatus", RoomStatusEnum.AVAILABLE).setParameter("inRoomTypeEntity", newReservation.getRoomTypeName());
        Query queryUnavailable = em.createQuery("SELECT r FROM RoomEntity r WHERE r.roomStatusEnum = :inRoomStatus  AND r.roomTypeEntity.roomTypeName = :inRoomTypeEntity").setParameter("inRoomStatus", RoomStatusEnum.UNAVAILABLE).setParameter("inRoomTypeEntity", newReservation.getRoomTypeName());
        List<RoomEntity> listOfRoomEntities = query.getResultList();
        listOfRoomEntities.addAll(queryUnavailable.getResultList());
        int currInventory = listOfRoomEntities.size();
        query = em.createQuery("SELECT r FROM ReservationEntity r WHERE r.reservationEndDate > :inDate AND r.roomTypeName = :inRoomTypeName").setParameter("inDate", LocalDateTime.now()).setParameter("inRoomTypeName", newReservation.getRoomTypeName());
        List<ReservationEntity> list = query.getResultList();
        LocalDateTime startDate = newReservation.getReservationStartDate();
        LocalDateTime endDate = newReservation.getReservationEndDate();
        for (ReservationEntity res : list) {
            LocalDateTime resStartDate = res.getReservationStartDate();
            LocalDateTime resEndDate = res.getReservationEndDate();
            if ((resStartDate.isAfter(startDate) && resStartDate.isBefore(endDate)) || (resEndDate.isAfter(startDate) && resEndDate.isBefore(endDate)) || ((resStartDate.isAfter(startDate) || resStartDate.isEqual(startDate)) && (resEndDate.isBefore(endDate)) || resEndDate.isEqual(endDate))) {
                currInventory -= 1;
            }
        }
        if (currInventory == 0) {
            throw new CreateNewReservationException();
        }
        //throw new CreateNewReservationException();
        //
        if (constraintViolations.isEmpty()) {
            try {
                for (String roomRateName : listOfRoomRateNames) {
                    RoomRateEntity roomRate;
                    try {
                        roomRate = roomRateEntitySessionBeanLocal.retrieveRoomRateByName(roomRateName);
                        newReservation.getRoomRateEntities().add(roomRate);
                    } catch (RoomRateNotFoundException ex) {
                        Logger.getLogger(ReservationEntitySessionBean.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                em.persist(newReservation);
                em.flush();

                return newReservation.getReservationEntityId();
            } catch (PersistenceException ex) {
                throw new UnknownPersistenceException(ex.getMessage());
            }
        } else {
            throw new InputDataValidationException(prepareInputDataValidationErrorsMessage(constraintViolations));
        }
    }

    @Override
    public Long createNewReservationForGuest(ReservationEntity newReservation, List<String> listOfRoomRateNames, GuestEntity guest) throws CreateNewReservationException, UnknownPersistenceException, InputDataValidationException {
        Set<ConstraintViolation<ReservationEntity>> constraintViolations = validator.validate(newReservation);
        //CHECK INVENTORY FOR THAT ROOMTYPE ONCE MORE BEFORE PERSISTING RESERVATION
        Query query = em.createQuery("SELECT r FROM RoomEntity r WHERE r.roomStatusEnum = :inRoomStatus AND r.roomTypeEntity.roomTypeName = :inRoomTypeEntity").setParameter("inRoomStatus", RoomStatusEnum.AVAILABLE).setParameter("inRoomTypeEntity", newReservation.getRoomTypeName());
        Query queryUnavailable = em.createQuery("SELECT r FROM RoomEntity r WHERE r.roomStatusEnum = :inRoomStatus  AND r.roomTypeEntity.roomTypeName = :inRoomTypeEntity").setParameter("inRoomStatus", RoomStatusEnum.UNAVAILABLE).setParameter("inRoomTypeEntity", newReservation.getRoomTypeName());
        List<RoomEntity> listOfRoomEntities = query.getResultList();
        listOfRoomEntities.addAll(queryUnavailable.getResultList());
        int currInventory = listOfRoomEntities.size();
        query = em.createQuery("SELECT r FROM ReservationEntity r WHERE r.reservationEndDate > :inDate AND r.roomTypeName = :inRoomTypeName").setParameter("inDate", LocalDateTime.now()).setParameter("inRoomTypeName", newReservation.getRoomTypeName());
        List<ReservationEntity> list = query.getResultList();
        LocalDateTime startDate = newReservation.getReservationStartDate();
        LocalDateTime endDate = newReservation.getReservationEndDate();
        for (ReservationEntity res : list) {
            LocalDateTime resStartDate = res.getReservationStartDate();
            LocalDateTime resEndDate = res.getReservationEndDate();
            if ((resStartDate.isAfter(startDate) && resStartDate.isBefore(endDate)) || (resEndDate.isAfter(startDate) && resEndDate.isBefore(endDate)) || ((resStartDate.isAfter(startDate) || resStartDate.isEqual(startDate)) && (resEndDate.isBefore(endDate)) || resEndDate.isEqual(endDate))) {
                currInventory -= 1;
            }
        }
        if (currInventory == 0) {
            throw new CreateNewReservationException();
        }
        //throw new CreateNewReservationException();
        //
        if (constraintViolations.isEmpty()) {
            try {
                for (String roomRateName : listOfRoomRateNames) {
                    RoomRateEntity roomRate;
                    try {
                        roomRate = roomRateEntitySessionBeanLocal.retrieveRoomRateByName(roomRateName);
                        newReservation.getRoomRateEntities().add(roomRate);
                    } catch (RoomRateNotFoundException ex) {
                        Logger.getLogger(ReservationEntitySessionBean.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                GuestEntity guestEntity = em.find(GuestEntity.class, guest.getUserEntityId());
                guestEntity.getReservationEntities().add(newReservation);

                em.persist(newReservation);
                em.flush();

                return newReservation.getReservationEntityId();
            } catch (PersistenceException ex) {
                throw new UnknownPersistenceException(ex.getMessage());
            }
        } else {
            throw new InputDataValidationException(prepareInputDataValidationErrorsMessage(constraintViolations));
        }
    }

    @Override
    public void createNewReservations(List<Pair<ReservationEntity, List<String>>> list) throws CreateNewReservationException, UnknownPersistenceException, InputDataValidationException {
        for (Pair<ReservationEntity, List<String>> pair : list) {
            try {
                createNewReservation(pair.getKey(), pair.getValue());
            } catch (CreateNewReservationException ex) {
                eJBContext.setRollbackOnly();
                throw new CreateNewReservationException();
            }
        }
    }

    @Override
    public void createNewReservationsForGuest(List<Pair<ReservationEntity, List<String>>> list, GuestEntity guest) throws CreateNewReservationException, UnknownPersistenceException, InputDataValidationException {
        for (Pair<ReservationEntity, List<String>> pair : list) {
            try {
                createNewReservationForGuest(pair.getKey(), pair.getValue(), guest);
            } catch (CreateNewReservationException ex) {
                eJBContext.setRollbackOnly();
                throw new CreateNewReservationException();
            }
        }
    }

    @Override
    public List<ReservationEntity> retrieveAllReservations() {
        Query query = em.createQuery("SELECT r FROM ReservationEntity r");

        List<ReservationEntity> listReservations = query.getResultList();
        for (ReservationEntity reservationEntity : listReservations) {
            reservationEntity.getRoomEntity();
            reservationEntity.getRoomRateEntities().size();
        }

        return listReservations;
    }

    @Override
    public ReservationEntity retrieveReservationById(Long reservationId) throws ReservationNotFoundException {

        ReservationEntity reservation = em.find(ReservationEntity.class, reservationId);
        if (reservation != null) {
            if (reservation.getRoomEntity() != null) {
                reservation.getRoomEntity();
                reservation.getRoomRateEntities().size();
            }
            return reservation;
        } else {
            throw new ReservationNotFoundException("Reservation Id: " + reservationId + " does not exist");
        }
    }

    @Override
    public List<ReservationEntity> retrieveReservationByPassportNumber(String passportNumber) {

        Query query = em.createQuery(
                "SELECT r FROM ReservationEntity r WHERE r.passportNumber = :passportNum")
                .setParameter("passportNum", passportNumber);

        List<ReservationEntity> reservations = query.getResultList();

        for (ReservationEntity reservation : reservations) {
            reservation.getRoomEntity();
            reservation.getRoomRateEntities().size();
        }
        return reservations;
    }

    @Override
    public List<ReservationEntity> retrieveReservationByPassportNumberForCheckIn(String passportNumber) {
        LocalDateTime checkInDate = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        Query query = em.createQuery(
                "SELECT r FROM ReservationEntity r WHERE r.passportNumber = :passportNum AND r.reservationStartDate = :inDate")
                .setParameter("passportNum", passportNumber)
                .setParameter("inDate", checkInDate);

        List<ReservationEntity> reservations = query.getResultList();

        for (ReservationEntity reservation : reservations) {
            reservation.getRoomEntity();
            reservation.getRoomRateEntities().size();
        }
        return reservations;
    }

    @Override
    public List<ReservationEntity> retrieveReservationByPassportNumberForCheckOut(String passportNumber) {
        LocalDateTime checkInDate = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        Query query = em.createQuery(
                "SELECT r FROM ReservationEntity r WHERE r.passportNumber = :passportNum AND r.reservationEndDate = :inDate")
                .setParameter("passportNum", passportNumber)
                .setParameter("inDate", checkInDate);

        List<ReservationEntity> reservations = query.getResultList();

        for (ReservationEntity reservation : reservations) {
            reservation.getRoomEntity();
            reservation.getRoomRateEntities().size();
        }
        return reservations;
    }

    @Override
    public List<ReservationEntity> retrieveAllReservationsWithStartDate(LocalDateTime startDate
    ) {
        Query query = em.createQuery("SELECT r FROM ReservationEntity r WHERE r.reservationStartDate = :inDate").setParameter("inDate", startDate);

        List<ReservationEntity> reservations = query.getResultList();

        for (ReservationEntity reservation : reservations) {
            reservation.getRoomEntity();
            reservation.getRoomRateEntities().size();
        }

        return reservations;

    }

    @Override
    public List<ReservationEntity> retrieveAllReservationsWithEndDate(LocalDateTime endDate) {
        Query query = em.createQuery("SELECT r FROM ReservationEntity r WHERE r.reservationEndDate = :inDate").setParameter("inDate", endDate);

        List<ReservationEntity> reservations = query.getResultList();

        for (ReservationEntity reservation : reservations) {
            reservation.getRoomEntity();
            reservation.getRoomRateEntities().size();
        }

        return reservations;

    }

    @Override
    public void deleteReservation(ReservationEntity reservationToDelete) throws ReservationNotFoundException {
        ReservationEntity reservation = retrieveReservationById(reservationToDelete.getReservationEntityId());
        if (reservation != null) {
            reservation.setRoomEntity(null);

            em.remove(reservation);
        } else {
            throw new ReservationNotFoundException("Reservation Id: " + reservation.getReservationEntityId() + " does not exist");
        }
    }

    @Override
    public void updateReservation(ReservationEntity reservation) throws ReservationNotFoundException, UpdateReservationException, InputDataValidationException {

        if (reservation != null && reservation.getReservationEntityId() != null) {
            Set<ConstraintViolation<ReservationEntity>> constraintViolations = validator.validate(reservation);

            if (constraintViolations.isEmpty()) {

                ReservationEntity reservationToUpdate = retrieveReservationById(reservation.getReservationEntityId());

                if (reservationToUpdate.getReservationEntityId().equals(reservation.getReservationEntityId())) {
                    reservationToUpdate.setReservationStartDate(reservation.getReservationStartDate());
                    reservationToUpdate.setReservationEndDate(reservation.getReservationEndDate());
                    reservationToUpdate.setFirstName(reservation.getFirstName());
                    reservationToUpdate.setLastName(reservation.getLastName());
                    reservationToUpdate.setEmail(reservation.getEmail());
                    reservationToUpdate.setContactNumber(reservation.getContactNumber());
                    reservationToUpdate.setPassportNumber(reservation.getPassportNumber());
                } else {
                    throw new UpdateReservationException("Reservation Id of reservation record does not match the existing record");
                }

            } else {
                throw new InputDataValidationException(prepareInputDataValidationErrorsMessage(constraintViolations));
            }
        } else {
            throw new ReservationNotFoundException("Rservation Id: " + reservation.getReservationEntityId() + " does not exist");
        }
    }

    @Override
    public HashMap<String, HashMap<String, BigDecimal>> retrieveAvailableRoomTypes(LocalDateTime startDate, LocalDateTime endDate,
            Integer numRooms) throws InsufficientRoomsAvailableException {
        System.out.println("Retrieving Room Types");
        //GET TOTAL INVENTORY
        //must take into account unavailable rooms as well, as they may just be unavailable now and not the day that you wan to make the booking
        Query query = em.createQuery("SELECT r FROM RoomEntity r WHERE r.roomStatusEnum = :inRoomStatus").setParameter("inRoomStatus", RoomStatusEnum.AVAILABLE);
        Query queryUnavailable = em.createQuery("SELECT r FROM RoomEntity r WHERE r.roomStatusEnum = :inRoomStatus").setParameter("inRoomStatus", RoomStatusEnum.UNAVAILABLE);
        List<RoomEntity> listOfRoomEntities = query.getResultList();
        listOfRoomEntities.addAll(queryUnavailable.getResultList());
        HashMap<String, HashMap<String, BigDecimal>> map = new HashMap<>();
        int totalRooms = listOfRoomEntities.size();
        for (RoomEntity room : listOfRoomEntities) {
            if (map.containsKey(room.getRoomTypeEntity().getRoomTypeName())) {
                HashMap<String, BigDecimal> stringToBigDecimalMap = map.get(room.getRoomTypeEntity().getRoomTypeName());
                stringToBigDecimalMap.put("numRoomType", stringToBigDecimalMap.get("numRoomType").add(BigDecimal.ONE));
                map.put(room.getRoomTypeEntity().getRoomTypeName(), stringToBigDecimalMap);
            } else {
                HashMap<String, BigDecimal> stringToBigDecimalMap = new HashMap<>();
                stringToBigDecimalMap.put("numRoomType", BigDecimal.ONE);
                Pair<List<RoomRateEntity>, BigDecimal> pair = calculatePriceOfStay(startDate, endDate, room.getRoomTypeEntity());

                stringToBigDecimalMap.put("bestPrice", pair.getValue());
                for (RoomRateEntity roomRate : pair.getKey()) {
                    stringToBigDecimalMap.put(roomRate.getRoomRateName(), BigDecimal.ONE);
                }
                map.put(room.getRoomTypeEntity().getRoomTypeName(), stringToBigDecimalMap);
            }
        }
        System.out.println("done getting initial inventory");
        //GET NUMBER OF ROOM USED
        int numRoomsUsed = 0;
        //only get reservations that endDate is already over so i dont have a huge list
        //query only reservations that HAVE NOT PASSED (NOW < ENDDATE)
        query = em.createQuery("SELECT r FROM ReservationEntity r WHERE r.reservationEndDate > :inDate").setParameter("inDate", LocalDateTime.of(LocalDate.now(), LocalTime.MIN));
        List<ReservationEntity> listOfReservationEntities = query.getResultList();
        System.out.println("getting whats used");
        for (ReservationEntity res : listOfReservationEntities) {
            LocalDateTime resStartDate = res.getReservationStartDate();
            LocalDateTime resEndDate = res.getReservationEndDate();
            if ((resStartDate.isAfter(startDate) && resStartDate.isBefore(endDate))
                    || (resEndDate.isAfter(startDate) && resEndDate.isBefore(endDate))
                    || ((resStartDate.isAfter(startDate) || resStartDate.isEqual(startDate))
                    && (resEndDate.isBefore(endDate)) || resEndDate.isEqual(endDate))) {
                HashMap<String, BigDecimal> stringToBigDecimalMap = map.get(res.getRoomTypeName());
                BigDecimal newNum = stringToBigDecimalMap.get("numRoomType").subtract(BigDecimal.ONE);
                numRoomsUsed += 1;
                //shouldnt be possible but just in case
                if (newNum.intValue() < 0) {
                    newNum = BigDecimal.ZERO;
                }
                stringToBigDecimalMap.put("numRoomType", newNum);
            }
        }
        if (totalRooms - numRoomsUsed < numRooms) {
            throw new InsufficientRoomsAvailableException();
        }
        System.out.println("Retrieved Room Types");
        return map;
    }

    @Override
    public HashMap<String, HashMap<String, BigDecimal>> retrieveAvailableRoomTypesOnline(LocalDateTime startDate, LocalDateTime endDate,
            Integer numRooms) throws InsufficientRoomsAvailableException {
        System.out.println("Retrieving Room Types");
        //GET TOTAL INVENTORY
        //must take into account unavailable rooms as well, as they may just be unavailable now and not the day that you wan to make the booking
        Query query = em.createQuery("SELECT r FROM RoomEntity r WHERE r.roomStatusEnum = :inRoomStatus").setParameter("inRoomStatus", RoomStatusEnum.AVAILABLE);
        Query queryUnavailable = em.createQuery("SELECT r FROM RoomEntity r WHERE r.roomStatusEnum = :inRoomStatus").setParameter("inRoomStatus", RoomStatusEnum.UNAVAILABLE);
        List<RoomEntity> listOfRoomEntities = query.getResultList();
        listOfRoomEntities.addAll(queryUnavailable.getResultList());
        HashMap<String, HashMap<String, BigDecimal>> map = new HashMap<>();
        int totalRooms = listOfRoomEntities.size();
        for (RoomEntity room : listOfRoomEntities) {
            if (map.containsKey(room.getRoomTypeEntity().getRoomTypeName())) {
                HashMap<String, BigDecimal> stringToBigDecimalMap = map.get(room.getRoomTypeEntity().getRoomTypeName());
                stringToBigDecimalMap.put("numRoomType", stringToBigDecimalMap.get("numRoomType").add(BigDecimal.ONE));
                map.put(room.getRoomTypeEntity().getRoomTypeName(), stringToBigDecimalMap);
            } else {
                HashMap<String, BigDecimal> stringToBigDecimalMap = new HashMap<>();
                stringToBigDecimalMap.put("numRoomType", BigDecimal.ONE);
                Pair<List<RoomRateEntity>, BigDecimal> pair = calculatePriceOfStayOnline(startDate, endDate, room.getRoomTypeEntity());

                stringToBigDecimalMap.put("bestPrice", pair.getValue());
                for (RoomRateEntity roomRate : pair.getKey()) {
                    stringToBigDecimalMap.put(roomRate.getRoomRateName(), BigDecimal.ONE);
                }
                map.put(room.getRoomTypeEntity().getRoomTypeName(), stringToBigDecimalMap);
            }
        }
        System.out.println("done getting initial inventory");
        //GET NUMBER OF ROOM USED
        int numRoomsUsed = 0;
        //COMPUTATION HEAVY how to only get reservations that endDate is already over so i dont have a huge list
        //query only reservations that HAVE NOT PASSED (NOW < ENDDATE)
        query = em.createQuery("SELECT r FROM ReservationEntity r WHERE r.reservationEndDate > :inDate").setParameter("inDate", LocalDateTime.of(LocalDate.now(), LocalTime.MIN));
        List<ReservationEntity> listOfReservationEntities = query.getResultList();
        System.out.println("getting whats used");
        for (ReservationEntity res : listOfReservationEntities) {
            LocalDateTime resStartDate = res.getReservationStartDate();
            LocalDateTime resEndDate = res.getReservationEndDate();
            if ((resStartDate.isAfter(startDate) && resStartDate.isBefore(endDate))
                    || (resEndDate.isAfter(startDate) && resEndDate.isBefore(endDate))
                    || ((resStartDate.isAfter(startDate) || resStartDate.isEqual(startDate))
                    && (resEndDate.isBefore(endDate)) || resEndDate.isEqual(endDate))) {
                HashMap<String, BigDecimal> stringToBigDecimalMap = map.get(res.getRoomTypeName());
                BigDecimal newNum = stringToBigDecimalMap.get("numRoomType").subtract(BigDecimal.ONE);
                numRoomsUsed += 1;
                //shouldnt be possible but just in case
                if (newNum.intValue() < 0) {
                    newNum = BigDecimal.ZERO;
                }
                stringToBigDecimalMap.put("numRoomType", newNum);
            }
        }
        if (totalRooms - numRoomsUsed < numRooms) {
            throw new InsufficientRoomsAvailableException();
        }
        System.out.println("Retrieved Room Types");
        return map;
    }

    private Pair<List<RoomRateEntity>, BigDecimal> calculatePriceOfStay(LocalDateTime startDate, LocalDateTime endDate, RoomTypeEntity roomTypeEntity) {
        //should only look at published rate, assume that there can only be 1 published rate and validity is forever
        BigDecimal totalPrice = BigDecimal.ZERO;
        LocalDateTime currDate = startDate;
        List<RoomRateEntity> list = new ArrayList<>();
        while (currDate.isBefore(endDate)) {
            //Query query = em.createQuery("SELECT rr FROM RoomRateEntity rr WHERE rr.roomTypeEntity.roomTypeId = :inRoomType AND rr.roomRateTypeEnum = :inRoomRateTypeEnum").setParameter("inRoomType", roomTypeEntity.getRoomTypeId()).setParameter("inRoomRateTypeEnum", RoomRateTypeEnum.PUBLISHED);
            //List<RoomRateEntity> listOfRoomRateEntities = query.getResultList();

            List<RoomRateEntity> listOfRoomRateEntities = roomTypeEntity.getRoomRateEntities();
//            BigDecimal lowest = BigDecimal.valueOf(99999);
//            RoomRateEntity lowestRoomRate = null;
            for (RoomRateEntity roomRate : listOfRoomRateEntities) {
                if (roomRate.getRoomRateTypeEnum() == RoomRateTypeEnum.PUBLISHED && roomRate.getIsDisabled() == false) {
                    list.add(roomRate);
                    totalPrice = totalPrice.add(roomRate.getRatePerNight());
                    break;
                }
            }
//                
//                if (((currDate.isAfter(roomRate.getValidPeriodFrom()) && currDate.isBefore(roomRate.getValidPeriodTo()))
//                        || currDate.isEqual(roomRate.getValidPeriodFrom()) || currDate.isEqual(roomRate.getValidPeriodTo())) && roomRate.getRoomRateTypeEnum().equals(RoomRateTypeEnum.PUBLISHED)) {
//                    if (roomRate.getRatePerNight().compareTo(lowest) < 0) {
//                        lowest = roomRate.getRatePerNight();
//                        lowestRoomRate = roomRate;
//                    }
//                }
//                totalPrice = totalPrice.add(lowest);
//                list.add(lowestRoomRate);
            currDate = currDate.plusDays(1);
        }
        Pair<List<RoomRateEntity>, BigDecimal> pair = new Pair<>(list, totalPrice);
        return pair;
    }

    private Pair<List<RoomRateEntity>, BigDecimal> calculatePriceOfStayOnline(LocalDateTime startDate, LocalDateTime endDate, RoomTypeEntity roomTypeEntity) {
        //must check between the 3 types of 
        BigDecimal totalPrice = BigDecimal.ZERO;
        LocalDateTime currDate = startDate;
        List<RoomRateEntity> list = new ArrayList<>();
        while (currDate.isBefore(endDate)) {
//            Query query = em.createQuery("SELECT rr FROM RoomRateEntity rr WHERE rr.roomTypeEntity.roomTypeId = :inRoomType AND rr.roomRateTypeEnum != :inRoomRateTypeEnum")
//                    .setParameter("inRoomType", roomTypeEntity.getRoomTypeId())
//                    .setParameter("inRoomRateTypeEnum", RoomRateTypeEnum.PUBLISHED);
//            List<RoomRateEntity> listOfRoomRateEntities = query.getResultList();
            List<RoomRateEntity> listOfRoomRateEntities = roomTypeEntity.getRoomRateEntities();
            RoomRateEntity normalRate = null;
            BigDecimal normalRatePrice = BigDecimal.ZERO;
            RoomRateEntity peakRate = null;
            BigDecimal peakRatePrice = BigDecimal.valueOf(99999);
            RoomRateEntity promoRate = null;
            BigDecimal promoRatePrice = BigDecimal.valueOf(99999);

//            BigDecimal lowest = BigDecimal.valueOf(99999);
//            RoomRateEntity lowestRoomRate = null;
            for (RoomRateEntity roomRate : listOfRoomRateEntities) {

                if (roomRate.getIsDisabled() == false) {
                    if (((currDate.isAfter(roomRate.getValidPeriodFrom()) && currDate.isBefore(roomRate.getValidPeriodTo()))
                            || currDate.isEqual(roomRate.getValidPeriodFrom())
                            || currDate.isEqual(roomRate.getValidPeriodTo()))
                            && !roomRate.getRoomRateTypeEnum().equals(RoomRateTypeEnum.PUBLISHED)) {

                        if (roomRate.getRoomRateTypeEnum() == RoomRateTypeEnum.NORMAL) {
                            normalRate = roomRate;
                            normalRatePrice = normalRate.getRatePerNight();
                        } else if (roomRate.getRoomRateTypeEnum() == RoomRateTypeEnum.PEAK) {
                            if (roomRate.getRatePerNight().compareTo(peakRatePrice) < 0) {
                                peakRatePrice = roomRate.getRatePerNight();
                                peakRate = roomRate;
                            }
                        } else if (roomRate.getRoomRateTypeEnum() == RoomRateTypeEnum.PROMOTION) {
                            if (roomRate.getRatePerNight().compareTo(promoRatePrice) < 0) {
                                promoRatePrice = roomRate.getRatePerNight();
                                promoRate = roomRate;
                            }
                        }
//                        if (roomRate.getRatePerNight().compareTo(lowest) < 0) {
//                            lowest = roomRate.getRatePerNight();
//                            lowestRoomRate = roomRate;
//                        }
                    }
                }

                if (promoRate != null) {
                    totalPrice = totalPrice.add(promoRatePrice);
                    list.add(promoRate);
                } else if (peakRate != null) {
                    totalPrice = totalPrice.add(peakRatePrice);
                    list.add(peakRate);
                } else {
                    totalPrice = totalPrice.add(normalRatePrice);
                    list.add(normalRate);
                }

                currDate = currDate.plusDays(1);
            }
        }
        Pair<List<RoomRateEntity>, BigDecimal> pair = new Pair<>(list, totalPrice);
        return pair;
    }

    @Override
    public void setReservationToCheckedIn(ReservationEntity reservationEntity) {
        ReservationEntity res = em.find(ReservationEntity.class,
                reservationEntity.getReservationEntityId());
        res.setIsCheckedIn(true);
        res.getRoomEntity().setRoomStatusEnum(RoomStatusEnum.UNAVAILABLE);

    }

    @Override
    public void setReservationToCheckedOut(ReservationEntity reservationEntity) {
        ReservationEntity res = em.find(ReservationEntity.class,
                reservationEntity.getReservationEntityId());
        res.setIsCheckedIn(false);
        res.getRoomEntity().setRoomStatusEnum(RoomStatusEnum.AVAILABLE);
        res.setRoomEntity(null);
    }

    private String prepareInputDataValidationErrorsMessage(Set<ConstraintViolation<ReservationEntity>> constraintViolations) {
        String msg = "Input data validation error!:";

        for (ConstraintViolation constraintViolation : constraintViolations) {
            msg += "\n\t" + constraintViolation.getPropertyPath() + " - " + constraintViolation.getInvalidValue() + "; " + constraintViolation.getMessage();
        }

        return msg;
    }
}
