/*
 * XLR8_module.c
 *
 * Created: 20-07-2015 10:07:35 PM
 *  Author: Ajinkya & Meet
 */ 

#define F_CPU 8000000
#include <avr/io.h>				// definations for registers
//default frequency of operation 1M
void USARTInit(unsigned int ubrr_value, uint8_t x2, uint8_t stopbits) {
	//from datasheet : 
	// Set baud rate
	UBRRL = ubrr_value & 255;
	UBRRH = ubrr_value >> 8;
	// Frame Format: asynchronous, 8 data bits, no parity, 1/2 stop bits
	UCSRC = _BV(UCSZ1) | _BV(UCSZ0);
	if(stopbits == 2) UCSRC |= _BV(USBS);
	if(x2) UCSRA = _BV(U2X); // 2x

	// USART Data Register Empty Interrupt Enable
	UCSRB = _BV(UDRIE);

	// Enable The receiver and transmitter
	UCSRB |= _BV(RXEN) | _BV(TXEN);
}

int main() {
	
	USARTInit(12, 0, 1); // initialise uart to baud rate 9600, no parity , 1 stopbit
	DDRB=0xFF;			// make all pins on port B as output
	PORTB  = 0x00;		// initially make all pins low
	
	while(1){		//endless loop
		while( !(UCSRA & (1<<RXC))) ;	// wait for data to be received in uart UDR register
		char temp = UDR;
		PORTB = temp;		//directly write value of uart data to port
		//temp = (temp& 0x0F)  | (( temp<<2)&0	x10);
		//PORTB = (temp&0x0F)|((temp<<4)&(0x0F));
		/*if ((temp&0x02)==2)
		{
			PORTB = 0xF0|(temp&0x0F);
		}
		else {
			PORTB = temp&0X0F;
		}*/
	}
}
