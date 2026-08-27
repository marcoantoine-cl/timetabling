package cl.colegio.timetabling.domain;

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

import java.util.List;

@PlanningSolution
public class Timetable {

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "timeSlotRange")
    private List<TimeSlot> timeSlotList;

    @ProblemFactCollectionProperty
    private List<Room> roomList;

    @ProblemFactCollectionProperty
    private List<Teacher> teacherList;

    @ProblemFactCollectionProperty
    private List<Curso> cursoList;

    @ProblemFactCollectionProperty
    private List<Ramo> ramoList;

    @PlanningEntityCollectionProperty
    private List<SesionRamo> sesionRamoList;

    @PlanningScore
    private HardSoftScore score;

    public Timetable() {
    }

    public Timetable(List<TimeSlot> timeSlotList, List<Room> roomList, List<Teacher> teacherList,
                      List<Curso> cursoList, List<Ramo> ramoList, List<SesionRamo> sesionRamoList) {
        this.timeSlotList = timeSlotList;
        this.roomList = roomList;
        this.teacherList = teacherList;
        this.cursoList = cursoList;
        this.ramoList = ramoList;
        this.sesionRamoList = sesionRamoList;
    }

    public List<TimeSlot> getTimeSlotList() {
        return timeSlotList;
    }

    public List<Room> getRoomList() {
        return roomList;
    }

    public List<Teacher> getTeacherList() {
        return teacherList;
    }

    public List<Curso> getCursoList() {
        return cursoList;
    }

    public List<Ramo> getRamoList() {
        return ramoList;
    }

    public List<SesionRamo> getSesionRamoList() {
        return sesionRamoList;
    }

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }
}
