// OOP Approach using prototypical inheritance, wrapped in a namespace object as required.
export const BaseTicket = function (route, price) {
    this.route = route;
    this.price = price;
};
BaseTicket.prototype.getSummary = function () {
    return `Ticket for ${this.route} costs ₹${this.price}.`;
};
export const BusTicket = function (route, price, seatNumber) {
    // Call base constructor
    BaseTicket.call(this, route, price);
    this.seatNumber = seatNumber;
};
// Set up prototypical inheritance using Object.create
BusTicket.prototype = Object.create(BaseTicket.prototype);
// Restore constructor reference
BusTicket.prototype.constructor = BusTicket;
// Add subclass method
BusTicket.prototype.getDetailedSummary = function () {
    return `${this.getSummary()} Assigned Seat: ${this.seatNumber}.`;
};
// Namespace object
export const BusKarooOOP = {
    BaseTicket,
    BusTicket
};
