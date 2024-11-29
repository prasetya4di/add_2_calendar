import Flutter
import UIKit
import EventKit
import EventKitUI
import Foundation

extension Date {
    init(milliseconds: Double) {
        self = Date(timeIntervalSince1970: TimeInterval(milliseconds) / 1000)
    }
}

public class Add2CalendarPlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "add_2_calendar", binaryMessenger: registrar.messenger())
        let instance = Add2CalendarPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if call.method == "add2Cal" {
            let args = call.arguments as! [String: Any]
            addEventToCalendar(from: args, result: result)
        }
    }

    private func addEventToCalendar(from args: [String: Any], result: @escaping FlutterResult) {
        let title = args["title"] as! String
        let description = args["desc"] as? String
        let location = args["location"] as? String
        let timeZone = TimeZone(identifier: args["timeZone"] as? String ?? "UTC")
        let startDate = Date(milliseconds: (args["startDate"] as! Double))
        let endDate = Date(milliseconds: (args["endDate"] as! Double))
        let alarmInterval = args["alarmInterval"] as? Double
        let allDay = args["allDay"] as! Bool
        let url = args["url"] as? String

        let eventStore = EKEventStore()
        let event = createEvent(eventStore: eventStore, alarmInterval: alarmInterval, title: title, description: description, location: location, timeZone: timeZone, startDate: startDate, endDate: endDate, allDay: allDay, url: url, args: args)

        presentCalendarModalToAddEvent(event, eventStore: eventStore, result: result)
    }

    private func createEvent(eventStore: EKEventStore, alarmInterval: Double?, title: String, description: String?, location: String?, timeZone: TimeZone?, startDate: Date?, endDate: Date?, allDay: Bool, url: String?, args: [String: Any]) -> EKEvent {
        let event = EKEvent(eventStore: eventStore)
        if let alarm = alarmInterval {
            event.addAlarm(EKAlarm(relativeOffset: alarm * (-1)))
        }
        event.title = title
        event.startDate = startDate
        event.endDate = endDate
        event.timeZone = timeZone
        event.location = location
        event.notes = description
        event.url = url != nil ? URL(string: url!) : nil
        event.isAllDay = allDay

        if let recurrence = args["recurrence"] as? [String: Any] {
            let interval = recurrence["interval"] as! Int
            let frequency = recurrence["frequency"] as! Int
            let end = recurrence["endDate"] as? Double
            let occurrences = recurrence["ocurrences"] as? Int

            let recurrenceRule = EKRecurrenceRule(
                recurrenceWith: EKRecurrenceFrequency(rawValue: frequency)!,
                interval: interval,
                end: occurrences != nil ? EKRecurrenceEnd(occurrenceCount: occurrences!) : end != nil ? EKRecurrenceEnd(end: Date(milliseconds: end!)) : nil
            )
            event.recurrenceRules = [recurrenceRule]
        }

        return event
    }

    private func presentCalendarModalToAddEvent(_ event: EKEvent, eventStore: EKEventStore, result: @escaping FlutterResult) {
        let eventModalVC = EKEventEditViewController()
        eventModalVC.event = event
        eventModalVC.eventStore = eventStore
        eventModalVC.editViewDelegate = self

        if #available(iOS 13, *) {
            eventModalVC.modalPresentationStyle = .fullScreen
        }

        // Use a completion closure to capture the result of the modal presentation
        if let root = UIApplication.shared.keyWindow?.rootViewController {
            root.present(eventModalVC, animated: true) {
                result(true) // Return true if the modal was presented successfully
            }
        } else {
            result(false) // Return false if there was an error presenting the modal
        }
    }
}

extension Add2CalendarPlugin: EKEventEditViewDelegate {
    public func eventEditViewController(_ controller: EKEventEditViewController, didCompleteWith action: EKEventEditViewAction) {
        controller.dismiss(animated: true)
    }
}