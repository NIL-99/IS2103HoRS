/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ejb.session.stateless;

import entity.ReservationEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import javax.ejb.Remote;
import util.exception.InputDataValidationException;
import util.exception.InsufficientRoomsAvailableException;
import util.exception.ReservationNotFoundException;
import util.exception.UnknownPersistenceException;
import util.exception.UpdateReservationException;

/**
 *
 * @author mingy
 */
@Remote
public interface ReservationEntitySessionBeanRemote {

    public Long createNewReservation(ReservationEntity newReservation, List<String> listOfRoomRateNames) throws UnknownPersistenceException, InputDataValidationException;

    public List<ReservationEntity> retrieveAllReservations();

    public ReservationEntity retrieveReservationById(Long reservationId) throws ReservationNotFoundException;

    public List<ReservationEntity> retrieveReservationByPassportNumber(String passportNumber);

    public void deleteReservation(ReservationEntity reservationToDelete) throws ReservationNotFoundException;

    public void updateReservation(ReservationEntity reservation) throws ReservationNotFoundException, UpdateReservationException, InputDataValidationException;

    public HashMap<String, HashMap<String, BigDecimal>> retrieveAvailableRoomTypes(LocalDateTime startDate, LocalDateTime endDate, Integer numRooms) throws InsufficientRoomsAvailableException;

}
