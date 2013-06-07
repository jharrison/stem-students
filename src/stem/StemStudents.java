package stem;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;


import masoncsc.datawatcher.*;
import masoncsc.util.Pair;

import edu.uci.ics.jung.graph.UndirectedSparseGraph;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Interval;
import stem.activities.Activity;
import stem.activities.ActivityType;
import stem.activities.ScienceClass;
import stem.network.*;
import stem.rules.RuleSet;

/**
 * An agent-based model of students interest in STEM with SYNERGIES project.
 * @author Joey Harrison
 * @author Matthew Hendrey
 * @version 0.1, October 12, 2012
 *
 */
public class StemStudents extends SimState
{
	private static final long serialVersionUID = 1L;

	public static final int NUM_ACTIVITY_TYPES = 16;
	public static final int NUM_TOPICS = 3;	
	
	public GregorianCalendar date;
	
	ArrayList<Student> students = new ArrayList<Student>();
	ArrayList<ActivityType> activityTypes = new ArrayList<ActivityType>();

	public ArrayList<Activity> scienceClasses = new ArrayList<Activity>();
	public ArrayList<Activity> activities = new ArrayList<Activity>();
	/** Indices of the activities, allows for shuffling to randomize the order. */
	private ArrayList<Integer> indices = new ArrayList<Integer>();
	
	public RuleSet ruleSet = new RuleSet();

	public UndirectedSparseGraph<Student, SimpleEdge> network = new UndirectedSparseGraph<Student, SimpleEdge>();

	
	ScreenDataWriter averageInterestScreenWriter;
	DoubleArrayWatcher averageInterestWatcher;
	DoubleArrayWatcher[] interestWatcher = new DoubleArrayWatcher[NUM_TOPICS];
	DoubleArrayWatcher activitiesDoneWatcher;

	TimeSeriesDataStore<Double> interest1Series = new TimeSeriesDataStore<Double>("Exploration Index");
	TimeSeriesDataStore<Double> interest2Series = new TimeSeriesDataStore<Double>("Science Index");
	TimeSeriesDataStore<Double> interest3Series = new TimeSeriesDataStore<Double>("Human Index");
	
	ArrayList<DataWatcher> dataWatchers = new ArrayList<DataWatcher>();
	
//	/** Counts of how many times a student has done each activity */
	int[] activityCounts = new int[NUM_ACTIVITY_TYPES];		
//	public int[] getActivityCounts() { return activityCounts; }
	
	/** Counts of how many times a female has done each activity */
	int[] activityGenderCounts = new int[NUM_ACTIVITY_TYPES];
	
//	/** Ratio of girls to boys among participants of each activity */
	double[] activityGenderRatios = new double[NUM_ACTIVITY_TYPES];
//	public double[] getActivityGenderRatios() { return activityGenderRatios; }
	
    String[] activityNames = new String[] { "Library", "Museum", "Scouts", "NationalParks", "Afterschool", 
    		"Talk", "SummerCamp", "Hike", "Garden", "Experiments", "Read", "Internet", "Computer", "TV", "Build", "Class" };
    public String[] getActivityNames() { return activityNames; }
	
	
	// Start getters/setters here =======================================================
	public int numStudents = 127;  //# from survey that have valid values
	public int getNumKids() { return numStudents; }
	public void setNumKids(int val) { numStudents = val; }

	public int classSize = 17; //Approx. # from data.  Adjusted slightly to get same number in each class.
	public int getClassSize() { return classSize; }
	public void setClassSize(int val) { classSize = val; }
	
	public int numFriendsPerStudent = 3;	
	public int getNumFriendsPerKid() { return numFriendsPerStudent; }
	public void setNumFriendsPerKid(int val) { numFriendsPerStudent = val; }

	public double smallWorldRewireProbability = 0.5;
	public double getSmallWorldRewireProbability() { return smallWorldRewireProbability; }
	public void setSmallWorldRewireProbability(double val) { smallWorldRewireProbability = val; }
	public Object domSmallWorldRewireProbability() { return new Interval(0.0, 1.0); }
	

	private double interGenderRewireProbability = 0.25;
	public double getInterGenderRewireProbability() { return interGenderRewireProbability; }
	public void setInterGenderRewireProbability(double val) { interGenderRewireProbability = val; }
	public Object domInterGenderRewireProbability() { return new Interval(0.0, 1.0); }

	public double interestThreshold = 0.75;
	public double getInterestThreshold() { return interestThreshold; }
	public void setInterestThreshold(double val) { interestThreshold = val; }
	public Object domInterestThreshold() { return new Interval(0.0, 1.0); }

	public double expertiseThreshold = 0.5;
	public double getExpertiseThreshold() { return expertiseThreshold; }
	public void setExpertiseThreshold(double val) { expertiseThreshold = val; }
	public Object domExpertiseThreshold() { return new Interval(0.0, 1.0); }

	public double passionThreshold = 0.5;
	public double getPassionThreshold() { return passionThreshold; }
	public void setPassionThreshold(double val) { passionThreshold = val; }
	public Object domPassionThreshold() { return new Interval(0.0, 1.0); }
	
	public double interestChangeRate = 0.01;
	public double getInterestChangeRate() { return interestChangeRate; }
	public void setInterestChangeRate(double val) { interestChangeRate = val; }
	public Object domInterestChangeRate() { return new Interval(0.0, 1.0); }

	public double interestDecayRate = 1.0;
	public double getInterestDecayExponent() { return interestDecayRate; }
	public void setInterestDecayExponent(double val) { interestDecayRate = val; }
	public Object domInterestDecayExponent() { return new Interval(0.0, 1.0); }
	
	public double nodeSize = 2.5;
	public double getNodeSize() { return nodeSize; }
	public void setNodeSize(double val) { nodeSize = val; }
	public Object domNodeSize() { return new Interval(0.0, 10.0); }

	public int maxActivitiesPerDay = 3;	
	public int getMaxActivitiesPerDay() { return maxActivitiesPerDay; }
	public void setMaxActivitiesPerDay(int val) { maxActivitiesPerDay = val; }
	
	public boolean randomizeInterests = false;
//	public boolean getRandomizeInterests() { return randomizeInterests; }
//	public void setRandomizeInterests(boolean val) { randomizeInterests = val; }
	
	public boolean randomizeStuffIDo = false;
//	public boolean getRandomizeStuffIDo() { return randomizeStuffIDo; }
//	public void setRandomizeStuffIDo(boolean val) { randomizeStuffIDo = val; }

	/** How much to change the participation rate in an activity if interest has
	 * been increased or decreased. */
	public double changeParticipationRate = 0.05;
	public double getChangeParticipationRate() { return changeParticipationRate; }
	public void setChangeParticipationRate(double val) { changeParticipationRate = val; }
	public Object domChangeParticipationRate() { return new Interval(0.0,0.5); }

	/** Probability of making a new friend when participating in an activity. */
	public double makeFriendProbability = 0.01;
	public double getMakeFriendProbability() { return makeFriendProbability; }
	public void setMakeFriendProbability(double val) { makeFriendProbability = val; }
	public Object domMakeFriendProbability() { return new Interval(0.0,0.5);}

	/** Probability of closing a triad, i.e. become friends with a friend of a friend. */
	public double closeTriadProbability = 0.05;
	public double getCloseTriadProbability() { return closeTriadProbability; }
	public void setCloseTriadProbability(double val) { closeTriadProbability = val; }
	public Object domCloseTriadProbability() { return new Interval(0.0,0.5); }
	
	

	public StemStudents(long seed) {
		super(seed);
		for (int i = 0; i < NUM_ACTIVITY_TYPES-1; i++)	// don't add an index for science class
			indices.add(i);
	}
	
	public void readInActivityTypes() throws IOException {
		activityTypes.clear();
		BufferedReader initActivities = null;
		
		initActivities = new BufferedReader(new FileReader ("./data/activityTypes.csv"));
		initActivities.readLine(); //Read in the header line of the file
		
		String line = null;
		while ((line = initActivities.readLine()) != null)
		{
			ActivityType a = ActivityType.parseActivityType(line);
			activityTypes.add(a);
		}	
	}
	
	public double clamp(double val, double min, double max) {
		if (val < min)
			return min;
		if (val > max)
			return max;
		return val;
	}
	
	/**
	 * Checks the model parameters and possibly randomize the given student's
	 * interests and stuffIDo.
	 * @param s
	 */
	public void possiblyRandomize(Student s) {
		if (randomizeInterests)
			s.interest = TopicVector.createRandom(random);
		
		if (randomizeStuffIDo)
			for (int i = 0; i < 15; i++)
				s.participationRates[i] = random.nextDouble();
	}

	public void initStudents() {
		students.clear();
		BufferedReader initInterests = null;
		
		/*
		 * Read in initial interests from data file, initialInterests.csv
		 */
		try {
			initInterests = new BufferedReader(new FileReader("./data/initialStudentInput.csv"));
			initInterests.readLine(); //Read in the header line of the file.
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		
		for (int i = 0; i < numStudents; i++) {
			String line = null;
			try {
				line = initInterests.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			/*
			 * TODO
			 * The model doesn't seem to handle trying to instantiate with more 
			 * youth than in the input file well.  The histograms have way too 
			 * many records in the 0 bin.
			 */
			if (line == null) {
				System.err.format("Error: input file only contains %d entries but numStudents is set to %d.\n", i, numStudents);
				break;
			}
			
			Student s = Student.parseStudent(this, line);
			possiblyRandomize(s);
			s.parent = new Adult(TopicVector.createRandom(random), TopicVector.createRandom(random));
			students.add(s);
		}
		// Close the buffered reader
		try {
			initInterests.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void initScienceClasses() {
		// init classes
		scienceClasses.clear();
		int numClasses = (int)Math.ceil(numStudents / (double)classSize);
		int studentIndex = 0;
		int totalStudents = 0;
		
		for (int i = 0; i < numClasses; i++) {
			// create activity for science class
			ScienceClass scienceClass = new ScienceClass();
			
			// add students
			for (int j = 0; (j < classSize) && (studentIndex < students.size()); j++)
				scienceClass.addParticipant(students.get(studentIndex++));
			
			// add teacher
			scienceClass.leaders.add(new Adult(TopicVector.createRandom(random), TopicVector.createRandom(random)));
			
			scienceClasses.add(scienceClass);
			totalStudents += scienceClass.potentialParticipants.size();

//			System.out.format("Science class: %s\n", scienceClass.content);
//			System.out.format("Teacher %s\n", scienceClass.leaders.get(0));
		}
		
		System.out.format("ScienceClasses: %d, total students: %d\n", scienceClasses.size(), totalStudents);
	}
	

	
	private void initStudentNetwork() {
		ArrayList<Student> females = new ArrayList<Student>();
		ArrayList<Student> males = new ArrayList<Student>();
		
		for (Student s : students)
			if (s.isFemale)
				females.add(s);
			else
				males.add(s);

		NetworkGenerator.initSmallWorldNetwork(females, numFriendsPerStudent, smallWorldRewireProbability, random);
		NetworkGenerator.initSmallWorldNetwork(males, numFriendsPerStudent, smallWorldRewireProbability, random);
		//NetworkGenerator.initSmallWorldNetwork(students, numFriendsPerStudent, smallWorldRewireProbability, random);
		
		//TODO write and use a function that only cross-links between two groups
		NetworkGenerator.rewireNetworkLinks(students, numFriendsPerStudent, interGenderRewireProbability, random);
		
					

		
		// Create and fill the JUNG network to use for display
		network = new UndirectedSparseGraph<Student, SimpleEdge>();
		for (Student p : students)
			network.addVertex(p);
		
		for (Student p1 : students) {
			for (Student p2 : p1.friends) {
				if (!network.isNeighbor(p1, p2))
					network.addEdge(new SimpleEdge(""), p1, p2);					
			}
		}
	}
	
	public boolean addFriends(Student a, Student b) {
		// can't become friends if they already are friends
		if (network.isNeighbor(a, b))
			return false;
		
		a.friends.add(b);
		b.friends.add(a);
		network.addEdge(new SimpleEdge(""), a, b);
		
		return true;
	}
	
	public boolean removeFriends(Student a, Student b) {
		// can't remove them if they aren't already friends
		SimpleEdge e = findEdge(a, b);
		if (e == null)
			return false;
		
		a.friends.remove(b);
		b.friends.remove(a);
		network.removeEdge(e);
		
		return true;
	}
	
	private SimpleEdge findEdge(Student a, Student b) {
		if (!network.isNeighbor(a, b))
			return null;
		
		for (SimpleEdge e : network.getEdges()) {
			Collection<Student> nodes = network.getIncidentVertices(e);
			if (nodes.contains(a) && nodes.contains(b))
				return e;
		}
		return null;
	}

	public void initDataLogging() {
		Arrays.fill(activityCounts, 0);
		dataWatchers.clear();
		
		averageInterestWatcher = new DoubleArrayWatcher() {
			// anonymous constructor
			{
				data = new double[numStudents];
			}

			@Override
			protected void updateDataPoint() {
				for (int i = 0; i < students.size(); i++)
					data[i] = students.get(i).getAverageInterest();				
			}
			
			@Override
			public String getCSVHeader() {
				return null;
			}
		};
		dataWatchers.add(averageInterestWatcher);

		for (int i = 0; i < NUM_TOPICS; i++) {
			final int topic = i;
			interestWatcher[i] = new DoubleArrayWatcher() {
				// anonymous constructor
				{
					data = new double[numStudents];
				}

				@Override
				protected void updateDataPoint() {
					for (int j = 0; j < students.size(); j++)
						data[j] = students.get(j).interest.topics[topic];				
				}
				
				@Override
				public String getCSVHeader() {
					return null;
				}
			};
			dataWatchers.add(interestWatcher[i]);
		}
		
		interest1Series.clear();
        dataWatchers.add(new PairDataWatcher<Long, Double>() {
            { addListener(interest1Series); }

            @Override
            protected void updateDataPoint() {
                final long currentStep = schedule.getSteps();
                double total = 0;
                for (Student s : students)
                	total += s.interest.topics[0];
                dataPoint = new Pair<Long, Double>(currentStep, (total / students.size()));
            }

            @Override
            public String getCSVHeader() {
                return "Step, " + interest1Series.getDescription();
            }
        });

        interest2Series.clear();
        dataWatchers.add(new PairDataWatcher<Long, Double>() {
            { addListener(interest2Series); }

            @Override
            protected void updateDataPoint() {
                final long currentStep = schedule.getSteps();
                double total = 0;
                for (Student s : students)
                	total += s.interest.topics[1];
                dataPoint = new Pair<Long, Double>(currentStep, (total / students.size()));
            }

            @Override
            public String getCSVHeader() {
                return "Step, " + interest2Series.getDescription();
            }
        });

        interest3Series.clear();
        dataWatchers.add(new PairDataWatcher<Long, Double>() {
            { addListener(interest3Series); }

            @Override
            protected void updateDataPoint() {
                final long currentStep = schedule.getSteps();
                double total = 0;
                for (Student s : students)
                	total += s.interest.topics[2];
                dataPoint = new Pair<Long, Double>(currentStep, (total / students.size()));
            }

            @Override
            public String getCSVHeader() {
                return "Step, " + interest3Series.getDescription();
            }
        });
        
		activitiesDoneWatcher = new DoubleArrayWatcher() {
			// anonymous constructor
			{
				data = new double[numStudents];
			}

			@Override
			protected void updateDataPoint() {
				for (int i = 0; i < students.size(); i++)
					data[i] = students.get(i).activitesDone / Math.max(schedule.getSteps(), 1.0);				
			}
			
			@Override
			public String getCSVHeader() {
				return null;
			}
		};
		dataWatchers.add(activitiesDoneWatcher);
		
	}
	
	/**
	 * Initialize the activity schedule based on survey data. Students were asked
	 * how often, from 1-5, they did certain activites outside of school. For now,
	 * we are interpretting these values to mean:
	 * 1: never
	 * 2: once every 30 days
	 * 3: once every 10 days
	 * 4: once every 3 days
	 * 5: every day
	 * 
	 * NOTE: This function employs static, fixed-interval scheduling
	 * 
	 */
//	public void initStaticSchedule() {
//		int[] intervals = new int[] { 0, 0, 30, 10, 3, 1 };	// it's one-based so stuff an extra zero in there
//
//		activities.clear();
//		for (Student s : students) {
//			for (int i = 0; i < NUM_ACTIVITY_TYPES; i++) {
//				if (s.stuffIDo[i] == 1)	// never
//					continue;
//				int daysBetween = intervals[s.stuffIDo[i]];
//
//				Activity a = Activity.createFromType(this, activityTypes.get(i));
//				a.addParticipant(s);
//				a.daysBetween = daysBetween;
//				if (a.isParentEncouraged)
//					a.leaders.add(s.parent);
//				activities.add(a);					
//			}
//		}
//		
//		for (Activity a : activities)
//			schedule.scheduleRepeating(a.daysBetween, a);
//		
//	}
	
	private void swap(int[] array, int i, int j) {
		int temp = array[i];
		array[i] = array[j];
		array[j] = temp;
	}
	
	public void temp() {
		int[] indices = new int[NUM_ACTIVITY_TYPES];
		for (int i = 0; i < NUM_ACTIVITY_TYPES; i++)
			indices[i] = i;

		for (int i = NUM_ACTIVITY_TYPES - 1; i > 0; i--)
			swap(indices, i, random.nextInt(i+1));
		
	}
	
	public boolean willDoToday(Student s, ActivityType type) {
		boolean schoolDay = isSchoolDay(date);
		boolean weekend = isWeekend(date);
		boolean summer = isSummer(date);

		// is this a valid day for this activity?
		if ((schoolDay && !type.onSchoolDay) ||
			(weekend && !type.onWeekendDay) ||
			(summer && !type.onSummer))
			return false;
		
		// have we done this activity too recently?
		if (s.daysSinceActivity[type.id] < type.daysBetween)
			return false;

		// stochastically decide whether to do this activity today or not
		if (random.nextDouble() < s.participationRates[type.id])
			return true;
		
		return false;
	}
	
	/**
	 * Schedule one day's worth of activities.
	 */
	public void doActivitiesForDay() {
		activities.clear();
		
		// loop through students in random order
		Collections.shuffle(students);
		for (Student s : students) {
			s.activities.clear();
			s.incrementCounters();
			// loop through activities in random order
			Collections.shuffle(indices);	
			for (int i : indices) {
				if (s.participationRates[i] == 0)	// student never does this one
					continue;

				// don't overschedule
				if (s.activities.size() >= maxActivitiesPerDay)
					break;

				ActivityType type = activityTypes.get(i);
				
				if (willDoToday(s, type))
					createOrJoinActivity(s, type);				
			}
		}
		
		// Now do them
		for (Activity a : activities) {
			a.step(this);
//			activityCounts[a.type.id]++;
		}
	}
	
	/**
	 * Find an activity to join. If there are no existing activities that match 
	 * the given type, return null. If there's a matching activity in which a 
	 * friend is participating, join that one. Otherwise, pick a random matching
	 * activity.
	 * @param s Student that wants to join an activity.
	 * @param type Type of the activity.
	 * @return The matching activity or null if none exists
	 */
	public Activity findActivityToJoin(Student s, ActivityType type) {
		ArrayList<Activity> matches = new ArrayList<Activity>();
		for (Activity a : activities)
			if ((a.type == type) && !a.isFull())
				matches.add(a);
				
		if (matches.size() == 0)
			return null;
				
		// check for friends
		for (Activity a : matches)
			if (a.contains(s.friends))
				return a;
					
		// if this is a friends-only activity and none of the matches contains a friend, don't join
		if (type.withFriendsOnly)
			return null;
		
		// randomly pick one of the matching open activities to join
		return matches.get(random.nextInt(matches.size()));
	}

	/**
	 * If a matching activity already exists, join it. Otherwise, create a new one.
	 * @param s Student that will be participating in the activity.
	 * @param type Type of the activity.
	 */
	public void createOrJoinActivity(Student s, ActivityType type) {
		Activity a = findActivityToJoin(s, type);
		
		if (a == null) {	
			a = Activity.createFromType(this, type);
			
			// if this activity involves a parent, add one
			if (type.numParents > 0)
				a.leaders.add(s.parent);
			
			// add leaders
			while (a.leaders.size() < type.numLeaders)
				a.leaders.add(new Adult(TopicVector.createRandom(random), TopicVector.createRandom(random)));
			
			activities.add(a);			
		}
		
		a.addParticipant(s);
		s.activities.add(a);
	}
	
	/** Event that is triggered when an activity is done. */
	public void studentParticipated(Student s, Activity activity) {
		int index = activity.type.id;
		
		activityCounts[index]++;
		
		if (s.isFemale)
			activityGenderCounts[index]++;
		
		activityGenderRatios[index] = activityGenderCounts[index] / (double)activityCounts[index];
	}
	
	void initStats() {

		Arrays.fill(activityCounts, 0);
		Arrays.fill(activityGenderCounts, 0);
		Arrays.fill(activityGenderRatios, 0.0);
		
	}
	
	public void printDayInfo() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        System.out.format("Step: %d, Date: %s, %d, %d, %d, %s, %s, %s\n", schedule.getSteps(), df.format(date.getTime()), 
        	date.get(Calendar.DAY_OF_WEEK), date.get(Calendar.DAY_OF_MONTH), date.get(Calendar.DAY_OF_YEAR),
        	isSchoolDay(date), isWeekend(date), isSummer(date));
	}
	
	@SuppressWarnings("serial")
	public void start() {
		super.start();
		date = new GregorianCalendar(2012, 8, 4);	// Sept 4th. Month is zero-based for some strange reason
		
		// Read in the characteristics of each activity
		try {
			readInActivityTypes();
			if (activityTypes.size() != NUM_ACTIVITY_TYPES)
				System.err.format("Error: %d activity types read from file, should be %d.\n", activityTypes.size(), NUM_ACTIVITY_TYPES);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Problem Reading in Activities");
		}

		initStudents();
		initScienceClasses();
		initStudentNetwork();
//		initStaticSchedule();		
		initDataLogging();
		initStats();

		//averageInterestScreenWriter = new ScreenDataWriter(averageInterestWatcher);

		
		System.out.format("scienceClasses.size(): %d\n", scienceClasses.size());
		for (Activity a : scienceClasses)
			schedule.scheduleRepeating(a);
		
		schedule.scheduleRepeating(new Steppable() {
			@Override
			public void step(SimState state) {
//				printDayInfo();
				doActivitiesForDay();
				decayInterests();
						
				for (DataWatcher<?> dw : dataWatchers)
					dw.update();
				
				date.add(Calendar.DATE, 1);
				
				if (state.schedule.getSteps() > 365)
					state.finish();
				
//				int totalFriendCount = 0;
//				for (Student s : students)
//					totalFriendCount += s.friends.size();
//				System.out.format("Network link count: %d, totalFriendCount: %d\n", network.getEdgeCount(), totalFriendCount);
			}
		});
	}
	
	/**
	 * Decay all the student's interest levels.
	 */
	public void decayInterests() {
		for (Student s : students)
			s.interest.scale(interestDecayRate);
	}
	
	/** Is the given day a school day? */
	private boolean isSchoolDay(Calendar date) {
		//TODO make this more comprehensive, e.g. exclude breaks
		return !isWeekend(date) && !isSummer(date);
	}

	/** Is the given day a weekend? */
	private boolean isWeekend(Calendar date) {
		int day = date.get(Calendar.DAY_OF_WEEK);
		if ((day == Calendar.SATURDAY) || (day == Calendar.SUNDAY))
			return true;
		return false;
	}

	/** Is the given day in the summer break? */
	private boolean isSummer(Calendar date) {
		int day = date.get(Calendar.DAY_OF_YEAR);
		if ((day > 158) && (day < 247))
			return true;
		
		return false;		
	}
	

	public static void main(String[] args) {
		doLoop(StemStudents.class, args);
		System.exit(0);
	}

}
