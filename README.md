# Departure app
 - Min sdk is 8.1. It was mentioned in interview as minimal version of OS on your platform.
 - App's developed with MVVM design pattern and clean architecture approach in mind. Asynchronous work is done with coroutines and presentation subscription is managed by LiveData.
 - StateFlow is used to switch Service process thread to UI thread. Business logic mapping happens inside Interactor on Default dispatcher thread pool and Views receives new data on Main dispatcher that starts collect of aforementioned Flow. This primitive's in Experimental status but works quite stable from my experience. With its' help you can set values outside from a Flow, also last emmited value is cached and internally 'distinctUntilChanged' logic is applied.
 - I'm not quite savvy with AIDL Services thus I do believe that my solution doesn't cover all cases about service lifecycle. Also codebase still has some magic numbers, hope their meanings would be clear. But room for improvements are still exists. I've left several TODOs.
 - To have smooth dial arrow animation ValueAnimator is used. It synchronizes own callback calls with UI choreographer not to overkill with unnecessary invalidate() calls.
 - To learn something new I've decided to try just graduated to release MotionLayout for inter-view animations.
![Dial](dial.png?raw=true "Title")